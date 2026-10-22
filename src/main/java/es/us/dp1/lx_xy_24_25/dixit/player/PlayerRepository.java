package es.us.dp1.lx_xy_24_25.dixit.player;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PlayerRepository extends CrudRepository<Player, Integer> {
    Optional<Player> findByUser_Username(String username);
}
