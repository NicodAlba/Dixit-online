package es.us.dp1.lx_xy_24_25.dixit.message;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Integer> {
    List<Message> findByPlayer(Player player);

    List<Message> findByGame(Game game);
}
