package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;


public interface FriendRequestRepository extends CrudRepository<FriendRequest, Integer> {
    List<FriendRequest> findByReceiver(User receiver);

    List<FriendRequest> findBySender(User sender);

    List<FriendRequest> findByReceiverAndDateAcceptedNotNull(User receiver);

    List<FriendRequest> findBySenderAndDateAcceptedNotNull(User sender);

    List<FriendRequest> findByReceiverAndDateAcceptedNull(User sender);

    List<FriendRequest> findBySenderAndDateAcceptedNull(User sender);

    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);
}
