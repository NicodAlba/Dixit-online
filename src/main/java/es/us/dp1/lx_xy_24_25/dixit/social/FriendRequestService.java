package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    @PersistenceContext private EntityManager entities;

    @Autowired
    public FriendRequestService(FriendRequestRepository friendRequestRepository) {
        this.friendRequestRepository = friendRequestRepository;
    }

    @Transactional
    public FriendRequest findById(Integer id) {
        return friendRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Friend request", "id", id));
    }

    @Transactional
    public FriendRequest save(FriendRequest friendRequest) {
        return friendRequestRepository.save(friendRequest);
    }

    @Transactional(readOnly = true)
    public List<User> getFriends(User user) {
        List<User> friends = new java.util.ArrayList<>(friendRequestRepository.findByReceiverAndDateAcceptedNotNull(user)
            .stream().map(FriendRequest::getSender).toList());
        friends.addAll(friendRequestRepository.findBySenderAndDateAcceptedNotNull(user)
            .stream().map(FriendRequest::getReceiver).toList());
        return friends;
    }

    @Transactional(readOnly = true)
    public List<FriendView> friendViews(User viewer) {
        return getFriends(viewer).stream().map(friend -> {
            FriendView.SpectatableGame available = viewer.getGamesToSpectate().stream()
                .filter(game -> game.getStartDate() != null && game.getEndDate() == null)
                .filter(game -> game.getSpectators().stream().anyMatch(u -> u.getId().equals(viewer.getId())))
                .sorted(java.util.Comparator.comparing(es.us.dp1.lx_xy_24_25.dixit.game.Game::getId).reversed())
                .flatMap(game -> game.getPlayers().stream()
                    .filter(player -> player.getUser().getId().equals(friend.getId()))
                    .map(player -> new FriendView.SpectatableGame(game.getId(), player.getId())))
                .findFirst().orElse(null);
            return new FriendView(friend.getId(), friend.getUsername(), available);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipView> relationships(User actor) {
        Set<FriendRequest> requests = new LinkedHashSet<>(friendRequestRepository.findBySender(actor));
        requests.addAll(friendRequestRepository.findByReceiver(actor));
        return requests.stream().map(request -> {
            boolean outgoing = Objects.equals(request.getSender().getId(), actor.getId());
            User other = outgoing ? request.getReceiver() : request.getSender();
            String status = request.getDateAccepted() != null ? "FRIEND" : outgoing ? "OUTGOING" : "INCOMING";
            return new FriendshipView(other.getId(), other.getUsername(), status, request.getId());
        }).toList();
    }

    @Transactional
    public void sendFriendRequest(User sender, User receiver) {
        if (Objects.equals(sender.getId(), receiver.getId())) throw new IllegalArgumentException("You cannot send yourself a friend request.");
        // Serialize both directions of the same pair without changing the friendship model.
        entities.find(User.class, Math.min(sender.getId(), receiver.getId()), LockModeType.PESSIMISTIC_WRITE);
        entities.find(User.class, Math.max(sender.getId(), receiver.getId()), LockModeType.PESSIMISTIC_WRITE);
        Optional<FriendRequest> existing = friendRequestRepository.findBySenderAndReceiver(sender, receiver)
            .or(() -> friendRequestRepository.findBySenderAndReceiver(receiver, sender));
        if (existing.isPresent()) {
            FriendRequest request = existing.get();
            if (request.getDateAccepted() != null) throw new IllegalStateException("You are already friends.");
            throw new IllegalStateException(Objects.equals(request.getSender().getId(), sender.getId())
                ? "Request already sent." : "You have a friend request from this player. Accept it instead.");
        }
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequestRepository.save(friendRequest);
    }

    @Transactional
    public void acceptFriendRequest(User receiver, FriendRequest request) {
        if (request.getDateAccepted() != null) {
            throw new IllegalStateException("You are already friends");
        }
        if (!request.getReceiver().equals(receiver)) {
            throw new AccessDeniedException("You are not allowed to accept this request");
        }
        request.setDateAccepted(new Date());
        friendRequestRepository.save(request);
    }

    @Transactional
    public void deleteFriendRequest(User user, FriendRequest request) {
        if (request.getDateAccepted() != null) {
            throw new IllegalStateException("You are already friends");
        }
        if (request.getSender().equals(user) || request.getReceiver().equals(user)) {
            friendRequestRepository.delete(request);
        } else  {
            throw new AccessDeniedException("You are not allowed to delete this request");
        }
    }

    @Transactional
    public void removeFriend(User user, FriendRequest request) {
        User sender = request.getSender();
        User receiver = request.getReceiver();

        if (user.equals(sender) || user.equals(receiver)) {
            friendRequestRepository.delete(request);
        } else  {
            throw new AccessDeniedException("You are not allowed to remove this request");
        }
    }

    @Transactional
    public void removeAllRequests(User user) {
        Set<FriendRequest> requests = new HashSet<>(friendRequestRepository.findByReceiver(user));
        requests.addAll(
            friendRequestRepository.findBySender(user)
        );
        friendRequestRepository.deleteAll(requests);
    }

    @Transactional(readOnly = true)
    public List<FriendRequest> findPendingRequests(User receiver) {
        return friendRequestRepository.findByReceiverAndDateAcceptedNull(receiver);
    }

    @Transactional(readOnly = true)
    public FriendRequest findRequestByUsers(User u1,  User u2) {
        Optional<FriendRequest> fr1 = friendRequestRepository.findBySenderAndReceiver(u1, u2);
        Optional<FriendRequest> fr2 = friendRequestRepository.findBySenderAndReceiver(u2, u1);
        if (fr1.isPresent()) {
            return fr1.get();
        } else if (fr2.isPresent()) {
            return fr2.get();
        } else {
            throw new ResourceNotFoundException("Friend request not found");
        }
    }

}
