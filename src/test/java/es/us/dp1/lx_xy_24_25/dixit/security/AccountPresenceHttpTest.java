package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties="dixit.host-disconnect-grace-ms=300")
class AccountPresenceHttpTest extends SecurityHttpSupport {
    @LocalServerPort int port;
    @Autowired JwtUtils signer;
    WebSocketStompClient client;
    List<StompSession> sessions=new ArrayList<>();
    @BeforeEach void transport() {
        client=new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new CompositeMessageConverter(List.of(new MappingJackson2MessageConverter(),new ByteArrayMessageConverter(),new StringMessageConverter())));client.start();
    }
    @AfterEach void stop(){
        try {for(var session:sessions)if(session.isConnected()) {
            try {session.disconnect();}
            catch(org.springframework.messaging.MessageDeliveryException failure){
                Throwable cause=failure.getCause();
                if(!(cause instanceof IllegalStateException) || cause.getMessage()==null
                    || !(cause.getMessage().contains("is not open: current state CLOSING")
                      || cause.getMessage().contains("is not open: current state CLOSED")))throw failure;
            }
        }} finally {client.stop();}
    }
    StompSession connect(String token)throws Exception {
        var headers=new StompHeaders();headers.add("Authorization","Bearer "+token);
        var session=client.connectAsync("http://127.0.0.1:"+port+"/ws",new WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter(){}).get(5,TimeUnit.SECONDS);
        sessions.add(session);return session;
    }
    void friend(Account first,Account second) {
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+second.id(),first.token(),null));
        int id=count("select id from friend_requests where sender_id=? and receiver_id=?",first.id(),second.id());
        status(200,request(HttpMethod.PATCH,"/friends/"+id+"/accept",second.token(),null));
    }
    JsonNode presence(Account actor,Integer game) {
        var response=request(HttpMethod.GET,"/presence"+(game==null?"":"?gameId="+game),actor.token(),null);
        status(200,response);return response.getBody();
    }
    boolean online(Account actor,int target,Integer game) {
        for(JsonNode connection:presence(actor,game))if(connection.path("accountId").asInt()==target)return connection.path("online").asBoolean();
        fail("Authorized account missing from presence snapshot");return false;
    }
    void awaitOnline(Account actor,int target,Integer game,boolean expected)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
        do {if(online(actor,target,game)==expected)return;Thread.sleep(30);}while(System.nanoTime()<deadline);
        assertEquals(expected,online(actor,target,game));
    }
    @Test void loginAloneIsOfflineAndFriendsOnlySeeConfirmedConnectionsUntilTheLastTabCloses()throws Exception {
        Account actor=account(), buddy=account(), outsider=account();friend(actor,buddy);
        assertFalse(online(actor,buddy.id(),null));
        var one=connect(buddy.token());var two=connect(buddy.token());connect(outsider.token());
        awaitOnline(actor,buddy.id(),null,true);
        JsonNode snapshot=presence(actor,null);assertEquals(1,snapshot.size());
        Set<String> fields=new HashSet<>();snapshot.get(0).fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("accountId","online"),fields);
        one.disconnect();assertTrue(online(actor,buddy.id(),null));
        two.disconnect();awaitOnline(actor,buddy.id(),null,false);
        var returned=connect(buddy.token());awaitOnline(actor,buddy.id(),null,true);
        returned.disconnect();awaitOnline(actor,buddy.id(),null,false);
        status(401,request(HttpMethod.GET,"/presence",null,null));
        status(403,request(HttpMethod.GET,"/presence",admin().token(),null));
    }
    @Test void expiryRemovesPresenceWithoutAClientDisconnect()throws Exception {
        Account actor=account(), buddy=account();friend(actor,buddy);
        String key=(String)org.springframework.test.util.ReflectionTestUtils.getField(signer,"jwtSecret");
        String shortToken=Jwts.builder().setSubject(buddy.name()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+3000)).signWith(SignatureAlgorithm.HS512,key).compact();
        var expiring=connect(shortToken);awaitOnline(actor,buddy.id(),null,true);awaitOnline(actor,buddy.id(),null,false);
        long closedBy=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(expiring.isConnected()&&System.nanoTime()<closedBy)Thread.sleep(20);
        assertFalse(expiring.isConnected());
    }
    @Test void rejectedAuthenticationNeverCreatesOnlinePresence()throws Exception {
        Account actor=account(), buddy=account();friend(actor,buddy);
        byte[] otherKey=new byte[64];new java.security.SecureRandom().nextBytes(otherKey);
        String invalid=Jwts.builder().setSubject(buddy.name()).setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(SignatureAlgorithm.HS512,Base64.getEncoder().encodeToString(otherKey)).compact();
        assertThrows(ExecutionException.class,()->connect(invalid));assertFalse(online(actor,buddy.id(),null));
    }
    @Test void gamePresenceIsLimitedToAuthorizedParticipantsAndEligibleSpectators()throws Exception {
        Account host=account(), second=account(), third=account(), spectator=account(), outsider=account();
        for(var participant:List.of(host,second,third))friend(participant,spectator);
        int game=request(HttpMethod.POST,"/games",host.token(),Map.of()).getBody().path("id").asInt();
        for(var participant:List.of(second,third))status(200,request(HttpMethod.PATCH,"/games/"+game+"/join",participant.token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+game+"/start",host.token(),null));
        connect(second.token());awaitOnline(host,second.id(),game,true);
        assertEquals(3,presence(host,game).size());assertEquals(3,presence(spectator,game).size());
        status(403,request(HttpMethod.GET,"/presence?gameId="+game,outsider.token(),null));
        status(404,request(HttpMethod.GET,"/presence?gameId=2147483647",host.token(),null));
    }
    @Test void anApplicationConnectionDoesNotReplaceTheHostsGameSubscription()throws Exception {
        Account host=account(), second=account(), third=account();
        int game=request(HttpMethod.POST,"/games",host.token(),Map.of()).getBody().path("id").asInt();
        for(var participant:List.of(second,third))status(200,request(HttpMethod.PATCH,"/games/"+game+"/join",participant.token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+game+"/start",host.token(),null));
        var application=connect(host.token());var table=connect(host.token());
        var subscribed=new CountDownLatch(1);
        table.subscribe("/topic/game/"+game,new StompFrameHandler(){
            public Type getPayloadType(StompHeaders headers){return JsonNode.class;}
            public void handleFrame(StompHeaders headers,Object payload){subscribed.countDown();}
        });
        assertTrue(subscribed.await(5,TimeUnit.SECONDS));table.disconnect();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);JsonNode view;
        do {view=request(HttpMethod.GET,"/games/"+game,host.token(),null).getBody();if(view.path("cancelled").asBoolean())break;Thread.sleep(30);}while(System.nanoTime()<deadline);
        assertTrue(view.path("cancelled").asBoolean());assertTrue(application.isConnected());
        assertTrue(online(second,host.id(),game));assertEquals(0,view.path("winners").size());
        assertNotEquals("FINISHED",view.path("turns").get(0).path("state").asText());
    }
}
