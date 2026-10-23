package es.us.dp1.lx_xy_24_25.dixit.statistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.sql.init.mode=never","spring.jpa.show-sql=false"})
class StatisticsIdentityTest {
    @Autowired StatisticsRepository statistics;
    @Autowired JdbcTemplate jdbc;
    static final int TRICKSTER=1001, STORYTELLER=1002;
    @BeforeEach void historicalRound(){
        jdbc.update("insert into authorities(id,authority) values(99,'PLAYER')");
        for(int id:List.of(TRICKSTER,STORYTELLER,1003))jdbc.update("insert into appusers(id,username,password,authority_id) values(?,?,?,99)",id,"fixture_"+id,"not-a-login-fixture");
        jdbc.update("insert into games(id,leader_id,start_date,end_date,cancelled) values(3001,1002,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,false)");
        // Storyteller's Player.id deliberately equals another account's User.id.
        jdbc.update("insert into players(id,user_id,game_id,points) values(1001,1002,3001,0),(2001,1001,3001,3),(2003,1003,3001,3)");
        jdbc.update("insert into images(id,url) values(4001,'fixture/1'),(4002,'fixture/2')");
        jdbc.update("insert into turns(id,game_id,storyteller_id,state) values(5001,3001,1001,'FINISHED')");
        jdbc.update("insert into cards(id,game_id,turn_id,user_id,image_id) values(6001,3001,5001,1002,4001),(6002,3001,5001,1001,4002)");
        jdbc.update("update turns set answer_id=6001 where id=5001");
        jdbc.update("insert into player_votes(card_id,player_id) values(6001,2001),(6002,2003)");
    }
    Map<String,Long> totals(List<Object[]> rows){Map<String,Long> values=new HashMap<>();rows.forEach(row->values.put((String)row[0],((Number)row[1]).longValue()));return values;}
    @Test void accountStatisticsIncludeRealTricksAndExcludeNarratorEvenWhenIdentifiersCollide(){
        assertAll(
            ()->assertEquals(1L,statistics.totalTrapVotesReceivedByUserId(TRICKSTER)),
            ()->assertEquals(1L,statistics.totalTurnsAsNormalPlayerByUserId(TRICKSTER)),
            ()->assertEquals(0L,statistics.totalTrapVotesReceivedByUserId(STORYTELLER)),
            ()->assertEquals(0L,statistics.totalTurnsAsNormalPlayerByUserId(STORYTELLER)),
            ()->assertEquals(0L,statistics.totalTrapVotesReceivedByUserId(987654)));
    }
    @Test void everyDeceptionRankingUsesAccountIdentityAndActualHistoricalCards(){
        Map<String,Long> expected=Map.of("fixture_1001",1L);
        assertAll(
            ()->assertEquals(expected,totals(statistics.findTop10ByTrapVotesReceived())),
            ()->assertEquals(expected,totals(statistics.findAllTrapVotes())),
            ()->assertEquals(expected,totals(statistics.findAllDeceptionOpportunities())));
        assertEquals(2,jdbc.queryForObject("select count(*) from cards where turn_id=5001 and player_id is null and user_id is not null",Integer.class));
    }
}
