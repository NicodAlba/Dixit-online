package es.us.dp1.lx_xy_24_25.dixit.security;

import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reuse cancellation, preserving stored rows and completed games; no new schema. */
class InterruptedGamePersistenceTest extends SecurityHttpSupport {
    @Autowired GameService games;
    @Test void repeatedStartupCancellationPreservesHistoryScoresAccountsAndFinishedGamesWithoutRewards() {
        var people=List.of(account(),account(),account());
        var host=people.getFirst();
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);
        int id=created.getBody().path("id").asInt();
        for(var guest:people.subList(1,3))status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",guest.token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/start",host.token(),null));
        int turn=count("select max(id) from turns where game_id=?",id);
        int player=count("select storyteller_id from turns where id=?",turn);
        int owner=count("select user_id from players where id=?",player);
        var narrator=people.stream().filter(p->p.id()==owner).findFirst().orElseThrow();
        int answer=count("select min(id) from cards where player_id=? and turn_id is null",player);
        status(200,request(HttpMethod.PUT,"/turns/"+turn+"/answer/"+answer,narrator.token(),null));
        status(200,request(HttpMethod.PUT,"/turns/"+turn+"/clue",narrator.token(),"Interrupted fixture"));
        var lobby=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,lobby);
        int lobbyId=lobby.getBody().path("id").asInt();
        // An ended fixture with real references must remain byte-for-byte untouched.
        jdbc.update("insert into games(id,leader_id,start_date,end_date,cancelled) values (?,?,?,?,false)",-900001,host.id(),new java.sql.Timestamp(1000),new java.sql.Timestamp(2000));
        var finished=jdbc.queryForList("select * from games where id=-900001").toString();
        jdbc.update("update players set points=7 where game_id=?",id);
        String before=history(id);int achievements=count("select count(*) from achievements"),users=count("select count(*) from appusers");
        games.cancelInterruptedGames();
        var cancelled=request(HttpMethod.GET,"/games/"+id,host.token(),null);status(200,cancelled);
        assertTrue(cancelled.getBody().path("cancelled").asBoolean());assertTrue(cancelled.getBody().hasNonNull("endDate"));
        var ended=cancelled.getBody().path("endDate");
        assertEquals(before,history(id));assertEquals(users,count("select count(*) from appusers"));
        assertEquals(achievements,count("select count(*) from achievements"));
        assertEquals(0,count("select count(*) from game_winners where game_id=?",id));
        assertEquals(finished,jdbc.queryForList("select * from games where id=-900001").toString());
        assertEquals(0,count("select count(*) from games where id=? and end_date is not null",lobbyId));
        assertEquals(0,games.cancelInterruptedGames());
        assertEquals(ended,request(HttpMethod.GET,"/games/"+id,host.token(),null).getBody().path("endDate"));
        status(201,request(HttpMethod.POST,"/games",host.token(),Map.of()));
        jdbc.update("delete from games where id=-900001");
    }
    private String history(int game) {
        return jdbc.queryForList("select * from cards where game_id=? order by id",game).toString()
            +jdbc.queryForList("select * from turns where game_id=? order by id",game)
            +jdbc.queryForList("select * from players where game_id=? order by id",game)
            +jdbc.queryForList("select * from player_votes where card_id in (select id from cards where game_id=?) order by card_id,player_id",game);
    }
}
