package es.us.dp1.lx_xy_24_25.dixit.game;


import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TurnRepository extends CrudRepository<Turn, Integer>{

    @Query("select t.storyteller from Turn t where t.id = :id")
    Player findTurnStorytellerById(@Param("id") Integer id);
    Optional<Turn> findById(Integer id);
    @Query("select t.game.id from Turn t where t.id = :id")
    Optional<Integer> findGameIdById(@Param("id") Integer id);
    Optional<Turn> findFirstByGameIdOrderByIdDesc(Integer gameId);
    List<Card> findPlayedCardsById(Integer id);
    //List<Turn> findAllTurnsByGameId(Integer id);
}
