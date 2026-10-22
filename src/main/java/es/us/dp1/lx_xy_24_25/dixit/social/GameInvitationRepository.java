package es.us.dp1.lx_xy_24_25.dixit.social;


import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface GameInvitationRepository extends CrudRepository<GameInvitation, Integer> {
    List<GameInvitation> findAllByInvited(User user);
}
