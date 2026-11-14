package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import es.us.dp1.lx_xy_24_25.dixit.game.*;
import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import es.us.dp1.lx_xy_24_25.dixit.statistics.StatisticsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.springframework.test.context.TestPropertySource(properties="dixit.host-disconnect-grace-ms=${DIXIT_ACCEPTANCE_GRACE_MS:2000}")
class GameCancellationTest extends SecurityHttpSupport {
    @org.springframework.beans.factory.annotation.Value("${dixit.host-disconnect-grace-ms}") long graceMs;
    @Autowired StatisticsRepository statistics;
    @Autowired AchievementService achievements;
    @Autowired UserService users;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @SpyBean GameRepository games;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    org.springframework.web.socket.messaging.WebSocketStompClient transport;
    List<org.springframework.messaging.simp.stomp.StompSession> sessions=new ArrayList<>();
    record Subscription(org.springframework.messaging.simp.stomp.StompSession session,java.util.concurrent.BlockingQueue<JsonNode> events) {}
    Subscription subscribe(Account actor,String destination)throws Exception{
        if(transport==null){
            var container=jakarta.websocket.ContainerProvider.getWebSocketContainer();
            transport=new org.springframework.web.socket.messaging.WebSocketStompClient(new org.springframework.web.socket.sockjs.client.SockJsClient(List.of(new org.springframework.web.socket.sockjs.client.WebSocketTransport(new org.springframework.web.socket.client.standard.StandardWebSocketClient(container)))));
            transport.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());transport.start();
        }
        var headers=new org.springframework.messaging.simp.stomp.StompHeaders();headers.add("Authorization","Bearer "+actor.token());
        var session=transport.connectAsync("http://127.0.0.1:"+port+"/ws",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter(){}).get(5,java.util.concurrent.TimeUnit.SECONDS);
        sessions.add(session);var events=new java.util.concurrent.LinkedBlockingQueue<JsonNode>();
        session.subscribe(destination,new org.springframework.messaging.simp.stomp.StompFrameHandler(){
            public java.lang.reflect.Type getPayloadType(org.springframework.messaging.simp.stomp.StompHeaders h){return JsonNode.class;}
            public void handleFrame(org.springframework.messaging.simp.stomp.StompHeaders h,Object payload){events.add((JsonNode)payload);}
        });
        assertNotNull(events.poll(5,java.util.concurrent.TimeUnit.SECONDS));return new Subscription(session,events);
    }
    @AfterEach void closeTransport(){for(var session:sessions)if(session.isConnected())session.disconnect();if(transport!=null)transport.stop();}
    void awaitCancellation(Match m)throws Exception{
        long limit=System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(graceMs+4000);
        while(System.nanoTime()<limit){if(view(m,m.people().getFirst()).path("cancelled").asBoolean())return;Thread.sleep(50);}
        fail("Expected cancellation after loss of the last host connection");
    }
    @Test void onlyLosingTheLastHostConnectionCancelsAndNotifiesAfterTheGrace()throws Exception{
        Match m=started();Account host=m.people().getFirst();int tid=view(m,host).path("turns").get(0).path("id").asInt();
        var hostGame=subscribe(host,"/topic/game/"+m.id());var hostTurn=subscribe(host,"/topic/turn/"+tid);
        var other=subscribe(m.people().get(1),"/topic/game/"+m.id());
        hostGame.session().disconnect();Thread.sleep(graceMs+250);assertTrue(view(m,host).path("endDate").isNull());
        String before=history(m.id());long lost=System.nanoTime();hostTurn.session().disconnect();awaitCancellation(m);
        assertTrue(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-lost)>=graceMs-100);
        JsonNode event=other.events().poll(5,java.util.concurrent.TimeUnit.SECONDS);assertNotNull(event);assertEquals("GAME_UPDATED",event.path("type").asText());assertEquals(2,event.size());
        assertEquals(before,history(m.id()));assertEquals(0,count("select count(*) from game_winners where game_id=?",m.id()));
    }
    @Test void reconnectWithinGraceRecoversMissedStateWithoutDuplicatedActions()throws Exception{
        Match m=started();Account host=m.people().getFirst();var connection=subscribe(host,"/topic/game/"+m.id());
        JsonNode initial=view(m,host);int tid=initial.path("turns").get(0).path("id").asInt(),story=initial.path("turns").get(0).path("storyteller").path("id").asInt();
        int uid=count("select user_id from players where id=?",story);Account narrator=m.people().stream().filter(a->a.id()==uid).findFirst().orElseThrow();
        int answer=count("select min(id) from cards where player_id=? and turn_id is null",story);
        connection.session().disconnect();Thread.sleep(150);
        status(200,request(HttpMethod.PUT,"/turns/"+tid+"/answer/"+answer,narrator.token(),null));
        status(200,request(HttpMethod.PUT,"/turns/"+tid+"/clue",narrator.token(),"missed while disconnected"));
        subscribe(host,"/topic/game/"+m.id());
        JsonNode recovered=view(m,host);assertEquals("missed while disconnected",recovered.path("turns").get(0).path("clue").asText());
        Thread.sleep(graceMs+200);assertTrue(view(m,host).path("endDate").isNull());assertEquals(1,count("select count(*) from turns where game_id=?",m.id()));assertEquals(36,count("select count(*) from cards where game_id=?",m.id()));
        assertEquals(1,count("select count(*) from cards where turn_id=?",tid));
    }
    @Test void nonHostDisconnectDoesNotCancelOrDeleteTheirSeat()throws Exception{
        Match m=started();subscribe(m.people().getFirst(),"/topic/game/"+m.id());
        var guest=subscribe(m.people().get(1),"/topic/game/"+m.id());String before=history(m.id());
        guest.session().disconnect();Thread.sleep(graceMs+200);
        assertTrue(view(m,m.people().getFirst()).path("endDate").isNull());assertEquals(3,count("select count(*) from players where game_id=?",m.id()));assertEquals(before,history(m.id()));
    }
    record Match(int id,List<Account> people) {}
    Match started(){
        var people=List.of(account(),account(),account());
        var created=request(HttpMethod.POST,"/games",people.getFirst().token(),Map.of("password","fixture"));status(201,created);int id=created.getBody().path("id").asInt();
        for(var p:people.subList(1,3))status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",p.token(),Map.of("password","fixture")));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/start",people.getFirst().token(),null));
        return new Match(id,people);
    }
    JsonNode view(Match m,Account actor){var r=request(HttpMethod.GET,"/games/"+m.id(),actor.token(),null);status(200,r);return r.getBody();}
    String history(int id){return jdbc.queryForList("select * from cards where game_id=? order by id",id).toString()+jdbc.queryForList("select * from player_votes where card_id in (select id from cards where game_id=?) order by card_id,player_id",id);}
    @Test void nonHostLeaveAndResumeKeepTheSameSeatAndCannotAdmitAnOutsider(){
        Match m=started();Account guest=m.people().get(1);String before=history(m.id());
        JsonNode own=view(m,guest);int pid=count("select id from players where game_id=? and user_id=?",m.id(),guest.id());
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",guest.token(),null));
        assertTrue(view(m,guest).path("endDate").isNull());
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/join",guest.token(),Map.of()));
        assertEquals(pid,count("select id from players where game_id=? and user_id=?",m.id(),guest.id()));
        assertEquals(own,view(m,guest));assertEquals(before,history(m.id()));
        assertEquals(3,count("select count(*) from players where game_id=?",m.id()));
        status(409,request(HttpMethod.PATCH,"/games/"+m.id()+"/join",account().token(),Map.of("password","fixture")));
        status(403,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",account().token(),null));
    }
    @Test void explicitHostLeaveCancelsExactlyOncePreservesUnscoredCardsAndAwardsNothing(){
        Match m=started();Account host=m.people().getFirst();JsonNode first=view(m,host).path("turns").get(0);
        int turn=first.path("id").asInt(),story=first.path("storyteller").path("id").asInt();
        int uid=count("select user_id from players where id=?",story);Account narrator=m.people().stream().filter(a->a.id()==uid).findFirst().orElseThrow();
        int card=count("select min(id) from cards where game_id=? and player_id=? and turn_id is null",m.id(),story);
        status(200,request(HttpMethod.PUT,"/turns/"+turn+"/answer/"+card,narrator.token(),null));
        String before=history(m.id());Long complete=statistics.countCompletedGames();
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",host.token(),null));
        JsonNode cancelled=view(m,host);assertTrue(cancelled.path("cancelled").asBoolean());assertFalse(cancelled.path("endDate").isNull());assertTrue(cancelled.path("winners").isEmpty());
        assertEquals(before,history(m.id()));assertEquals(1,count("select count(*) from turns where game_id=?",m.id()));assertEquals(36,count("select count(*) from cards where game_id=?",m.id()));
        assertEquals(complete,statistics.countCompletedGames());
        assertEquals(0L,statistics.countCompletedGamesByUserId(host.id()));assertNull(statistics.totalPointsByUserId(host.id()),"SUM excludes the cancelled game entirely");
        for(Account p:m.people()){
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{
                var user=users.findById(p.id());
                achievements.checkPlayAchievements(user);
                achievements.checkTrickAchievements(user); // Same transaction as the real winner-declaration consumer.
            });
            assertEquals(0,count("select count(*) from achievements where user_id=? and date_obtained is not null",p.id()));
        }
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",host.token(),null));
        assertEquals(cancelled,view(m,host));assertEquals(before,history(m.id()));
        status(400,request(HttpMethod.PUT,"/turns/"+turn+"/clue",narrator.token(),"late"));
        status(409,request(HttpMethod.PATCH,"/games/"+m.id()+"/join",m.people().get(1).token(),Map.of("password","fixture")));
    }
    @Test void failedCancellationRollsBackAndCanBeRetried(){
        Match m=started();String before=history(m.id());
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture cancellation failure")).when(games).save(argThat(g->g instanceof Game game&&Objects.equals(game.getId(),m.id())&&game.isCancelled()));
        try{status(500,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",m.people().getFirst().token(),null));}
        finally{reset(games);}
        assertFalse(view(m,m.people().getFirst()).path("cancelled").asBoolean());assertTrue(view(m,m.people().getFirst()).path("endDate").isNull());assertEquals(before,history(m.id()));
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/leave",m.people().getFirst().token(),null));assertTrue(view(m,m.people().getFirst()).path("cancelled").asBoolean());
    }
}
