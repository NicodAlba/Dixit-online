package es.us.dp1.lx_xy_24_25.dixit.card;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;

@Repository
public interface CardRepository extends CrudRepository<Card, Integer> {
    List<Card> findByGameId(Integer gameId);
    int countByGameAndOwnerIsNull(Game game);
}
