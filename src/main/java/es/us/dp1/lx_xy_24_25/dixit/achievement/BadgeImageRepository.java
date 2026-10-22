package es.us.dp1.lx_xy_24_25.dixit.achievement;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadgeImageRepository extends CrudRepository<BadgeImage, Integer>{
    List<BadgeImage> findAll();
    List<BadgeImage> findByType(AchievementType type);
}
