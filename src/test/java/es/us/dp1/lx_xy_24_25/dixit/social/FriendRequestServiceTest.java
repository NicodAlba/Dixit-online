package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Epic("Social")
@Feature("Friends")
class FriendRequestServiceTest {

    @Autowired
    private FriendRequestService friendRequestService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthoritiesService authoritiesService;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User sender;
    private User receiver;
    private User invalid;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUsername("sender");
        sender.setPassword("test");
        sender.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(sender);

        receiver = new User();
        receiver.setUsername("receiver");
        receiver.setPassword("test");
        receiver.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(receiver);

        invalid = new User();
        invalid.setUsername("invalid");
        invalid.setPassword("test");
        invalid.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(invalid);
    }

    @Test
    @Transactional
    @DisplayName("Should get friends")
    void getFriends() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());

        friendRequestService.save(friendRequest);
        List<User> friends = friendRequestService.getFriends(sender);

        assertNotNull(friends);
        assertEquals(1, friends.size());
        assertTrue(friends.stream().anyMatch(u -> u.getUsername().equals(receiver.getUsername())));
    }

    @Test
    @Transactional
    @DisplayName("Should not list non accepted requests as friends")
    void checkNotAcceptedFriendsNotListed() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);

        friendRequestService.save(friendRequest);
        List<User> friends = friendRequestService.getFriends(sender);

        assertEquals(0, friends.size());
    }

    @Test
    @Transactional
    @DisplayName("Should not let sender and receiver be the same")
    void checkSenderAndReceiverCantBeTheSame() {
        assertThrows(IllegalArgumentException.class, () -> {friendRequestService.sendFriendRequest(sender, sender);});
    }

    @Test
    @Transactional
    @DisplayName("Should accept friend request")
    void acceptFriendRequest() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        friendRequestService.acceptFriendRequest(receiver, friendRequestSaved);

        assertNotNull(friendRequestRepository.findById(friendRequestSaved.getId()).get().getDateAccepted());
    }

    @Test
    @Transactional
    @DisplayName("Should not allow to accept friend request")
    @Description("Should not accept request since users are already friends")
    void checkCantAcceptAlreadyFriends() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        assertThrows(IllegalStateException.class, () -> {friendRequestService.acceptFriendRequest(receiver, friendRequestSaved);});
    }

    @Test
    @Transactional
    @DisplayName("Should not allow to accept friend request if user is not the receiver")
    void checkCantAcceptIfNotReceiver() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        assertThrows(AccessDeniedException.class, () -> {friendRequestService.acceptFriendRequest(sender, friendRequestSaved);});
    }

    @Test
    @Transactional
    @DisplayName("Should remove friend")
    @Description("Should remove friend if user is set as sender of an accepted request")
    void removeFriendBySender() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        friendRequestService.removeFriend(sender, friendRequestSaved);

        assertThrows(ResourceNotFoundException.class, () -> {friendRequestService.findById(friendRequestSaved.getId());});
        assertEquals(0, friendRequestService.getFriends(sender).size());
    }

    @Test
    @Transactional
    @DisplayName("Should remove friend")
    @Description("Should remove friend if user is set as receiver of an accepted request")
    void removeFriendByReceiver() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        friendRequestService.removeFriend(receiver, friendRequestSaved);

        assertThrows(ResourceNotFoundException.class, () -> {friendRequestService.findById(friendRequestSaved.getId());});
        assertEquals(0, friendRequestService.getFriends(sender).size());

    }

    @Test
    @Transactional
    @DisplayName("Should not remove friend")
    @Description("Should not allow to remove friend if it doesn't belong to the user")
    void removeFriendByInvalidUser() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());

        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        assertThrows(AccessDeniedException.class, () -> {friendRequestService.removeFriend(invalid, friendRequestSaved);});
        assertEquals(1, friendRequestService.getFriends(sender).size());

    }

    @Test
    @Transactional
    void findRequestByUsers() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        assertThrows(ResourceNotFoundException.class, () -> {friendRequestService.findRequestByUsers(receiver, invalid);});
        assertEquals(friendRequestSaved.getId(), friendRequestService.findRequestByUsers(sender, receiver).getId());
    }

    @Test
    @Transactional
    void deleteFriendRequest() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        FriendRequest friendRequestSaved = friendRequestRepository.save(friendRequest);

        friendRequestService.deleteFriendRequest(receiver, friendRequestSaved);
        assertThrows(AccessDeniedException.class, () -> {friendRequestService.deleteFriendRequest(invalid, friendRequestSaved);});
        assertEquals(0, friendRequestService.getFriends(receiver).size());
    }

    @Test
    @Transactional
    void sendFriendRequest() {
        friendRequestService.sendFriendRequest(sender, receiver);
        assertEquals(1, friendRequestService.findPendingRequests(receiver).size());
    }

    @Test
    @Transactional
    void shouldAlreadyBeFriends() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setDateAccepted(new Date());
        friendRequestRepository.save(friendRequest);
        assertThrows(IllegalStateException.class, () -> {friendRequestService.sendFriendRequest(sender, receiver);});
    }

    @Test
    @Transactional
    void removeAllRequests() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequestRepository.save(friendRequest);
        assertEquals(1, friendRequestService.findPendingRequests(receiver).size());
        friendRequestService.removeAllRequests(receiver);
        assertEquals(0, friendRequestService.findPendingRequests(receiver).size());
    }

    @Test
    @Transactional
    void findPendingRequests() {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequestRepository.save(friendRequest);
        assertEquals(1, friendRequestService.findPendingRequests(receiver).size());
    }

}
