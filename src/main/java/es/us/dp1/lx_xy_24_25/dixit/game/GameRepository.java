package es.us.dp1.lx_xy_24_25.dixit.game;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends CrudRepository<Game, Integer>{
    List<Game> findAll();
    Optional<Game> findById(Integer id);
    Optional<Game> findByPassword(String password);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    Optional<Game> findByIdForUpdate(@Param("id") Integer id);
    @Query("SELECT g FROM Game g JOIN g.players gp WHERE gp.user.username = :username")
    List<Game> findGamesByPlayerUsername(@Param("username") String username);
    @Query("SELECT g FROM Game g JOIN g.winners gw WHERE gw.user.username = :username")
    List<Game> findGamesByWinnerUsername(@Param("username") String username);
    List<Game> findByEndDateIsNotNull();
    List<Game> findByStartDateNotNullAndEndDateIsNull();
    List<Game> findByEndDateNotNull();
}
