package es.us.dp1.lx_xy_24_25.dixit.achievement;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import es.us.dp1.lx_xy_24_25.dixit.user.User;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends CrudRepository<Achievement, Integer>{
    List<Achievement> findAll();
    Optional<Achievement> findById(Integer id);
    @Query("SELECT a FROM Achievement a WHERE a.user = ?1 AND TYPE(a) = ?2")
    List<Achievement> findByUserAndType(User user, Class<? extends Achievement> type);
    List<Achievement> findAllAchievementsByUserId(Integer id);
}
