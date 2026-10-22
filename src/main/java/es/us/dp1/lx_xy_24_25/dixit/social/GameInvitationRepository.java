package es.us.dp1.lx_xy_24_25.dixit.social;


import es.us.dp1.lx_xy_24_25.dixit.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface GameInvitationRepository extends CrudRepository<GameInvitation, Integer> {
    List<GameInvitation> findAllByInvited(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM GameInvitation i WHERE i.id = :id")
    Optional<GameInvitation> findByIdForUpdate(@Param("id") Integer id);
}
