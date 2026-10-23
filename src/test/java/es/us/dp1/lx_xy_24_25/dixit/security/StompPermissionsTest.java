package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.websocket.ContainerProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class StompPermissionsTest extends SecurityHttpSupport {
    @LocalServerPort int port;
    @Autowired es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.JwtUtils jwtSigner;
    String signingKey;
    @Autowired SimpUserRegistry registry;
    @Autowired SimpleBrokerMessageHandler broker;
    @Autowired SimpMessagingTemplate publisher;
    WebSocketStompClient client;
    List<Connection> connections=new ArrayList<>();
    record Connection(StompSession session,Signals signals) {}
    static class Signals extends StompSessionHandlerAdapter {
        final BlockingQueue<String> errors=new LinkedBlockingQueue<>();
        @Override public Type getPayloadType(StompHeaders h){return byte[].class;}
        @Override public void handleFrame(StompHeaders h,Object payload){errors.add("ERROR");}
        @Override public void handleException(StompSession s,StompCommand c,StompHeaders h,byte[] p,Throwable e){errors.add("ERROR");}
        @Override public void handleTransportError(StompSession s,Throwable e){errors.add("TRANSPORT");}
    }
    @BeforeEach void transport(){
        signingKey=(String)org.springframework.test.util.ReflectionTestUtils.getField(jwtSigner,"jwtSecret");
        var container=ContainerProvider.getWebSocketContainer();container.setDefaultMaxTextMessageBufferSize(512*1024);
        client=new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient(container)))));
        client.setInboundMessageSizeLimit(512*1024);
        client.setMessageConverter(new CompositeMessageConverter(List.of(new MappingJackson2MessageConverter(),new ByteArrayMessageConverter(),new StringMessageConverter())));
        client.start();
    }
    @AfterEach void close(){for(Connection c:connections)if(c.session().isConnected())c.session().disconnect();client.stop();}
    StompHeaders headers(String token){StompHeaders h=new StompHeaders();if(token!=null)h.add("Authorization","Bearer "+token);return h;}
    Connection connect(Account account)throws Exception{return connect(account.token());}
    Connection connect(String token)throws Exception{
        Signals signals=new Signals();StompSession session=client.connectAsync("http://127.0.0.1:"+port+"/ws",new WebSocketHttpHeaders(),headers(token),signals).get(5,TimeUnit.SECONDS);
        Connection c=new Connection(session,signals);connections.add(c);return c;
    }
    BlockingQueue<JsonNode> subscribe(Connection connection,Account account,String destination)throws Exception{
        BlockingQueue<JsonNode> events=new LinkedBlockingQueue<>();
        var subscription=connection.session().subscribe(destination,new StompFrameHandler(){
            public Type getPayloadType(StompHeaders h){return h.getContentType()!=null && org.springframework.http.MediaType.TEXT_PLAIN.isCompatibleWith(h.getContentType()) ? String.class : JsonNode.class;}
            public void handleFrame(StompHeaders h,Object payload){events.add(payload instanceof String text ? com.fasterxml.jackson.databind.node.TextNode.valueOf(text) : (JsonNode)payload);}
        });
        var h=SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);h.setDestination(destination);
        var message=MessageBuilder.createMessage(new byte[0],h.getMessageHeaders());
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline){
            var user=registry.getUser(account.name());var installed=broker.getSubscriptionRegistry().findSubscriptions(message);
            if(user!=null&&user.getSessions().stream().anyMatch(s->installed.getOrDefault(s.getId(),List.of()).contains(subscription.getSubscriptionId())))return events;
            if(!connection.signals().errors.isEmpty()) fail("Valid subscription was rejected");
            Thread.sleep(10);
        }
        fail("Valid subscription did not reach the broker");return events;
    }
    int lobby(Account host,Account guest){
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);int id=created.getBody().path("id").asInt();
        if(guest!=null)status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",guest.token(),Map.of()));return id;
    }
    String token(Account user,Date expiry,String key){return Jwts.builder().setSubject(user.name()).setIssuedAt(new Date()).setExpiration(expiry).signWith(SignatureAlgorithm.HS512,key).compact();}
    @Test void connectRejectsMissingInvalidWrongSignatureAndExpiredJwt()throws Exception{
        Account user=account();byte[] key=new byte[64];new java.security.SecureRandom().nextBytes(key);
        for(String jwt:Arrays.asList(null,"invalid-fixture",token(user,new Date(System.currentTimeMillis()-1000),signingKey),token(user,new Date(System.currentTimeMillis()+60000),Base64.getEncoder().encodeToString(key)))){
            Signals signals=new Signals();var pending=client.connectAsync("http://127.0.0.1:"+port+"/ws",new WebSocketHttpHeaders(),headers(jwt),signals);
            assertNotNull(signals.errors.poll(5,TimeUnit.SECONDS),"Unauthenticated CONNECT must be rejected");
            assertFalse(pending.isDone()&&!pending.isCompletedExceptionally()&&pending.getNow(null)!=null&&pending.getNow(null).isConnected());
            pending.cancel(true);
        }
    }
    @Test void membersAndSpectatorReceiveGameAndTurnButOutsiderAndAdminCannotSubscribe()throws Exception{
        Account host=account(),second=account(),third=account(),viewer=account();
        for(Account p:List.of(host,second,third)){
            status(200,request(HttpMethod.POST,"/friends/addFriend/"+viewer.id(),p.token(),null));
            int friendship=count("select id from friend_requests where sender_id=? and receiver_id=?",p.id(),viewer.id());
            status(200,request(HttpMethod.PATCH,"/friends/"+friendship+"/accept",viewer.token(),null));
        }
        int id=lobby(host,second);status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",third.token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/start",host.token(),null));
        int turn=count("select max(id) from turns where game_id=?",id);
        Connection member=connect(host),spectator=connect(viewer);
        var memberGame=subscribe(member,host,"/topic/game/"+id);var memberTurn=subscribe(member,host,"/topic/turn/"+turn);
        var viewerGame=subscribe(spectator,viewer,"/topic/game/"+id);var viewerTurn=subscribe(spectator,viewer,"/topic/turn/"+turn);
        publisher.convertAndSend("/topic/game/"+id,Map.of("id",id));publisher.convertAndSend("/topic/turn/"+turn,Map.of("id",turn));
        assertEquals(id,memberGame.poll(5,TimeUnit.SECONDS).path("id").asInt());assertEquals(id,viewerGame.poll(5,TimeUnit.SECONDS).path("id").asInt());
        assertEquals(turn,memberTurn.poll(5,TimeUnit.SECONDS).path("id").asInt());assertEquals(turn,viewerTurn.poll(5,TimeUnit.SECONDS).path("id").asInt());
        for(Account outsider:List.of(account(),admin()))for(String destination:List.of("/topic/game/"+id,"/topic/turn/"+turn)){
            Connection denied=connect(outsider);denied.session().subscribe(destination,new Signals());
            assertNotNull(denied.signals().errors.poll(5,TimeUnit.SECONDS),"An outsider must not subscribe");
        }
    }
    @ParameterizedTest @ValueSource(strings={"/topic/game/*","/topic/turn/*","/topic/game/1/extra","/topic/game/-1","/user/queue/private","/app/game/1"})
    void rejectsWildcardAndUnapprovedDestinations(String destination)throws Exception{
        Connection c=connect(account());c.session().subscribe(destination,new Signals());
        assertNotNull(c.signals().errors.poll(5,TimeUnit.SECONDS));
    }
    @Test void clientCannotForgeServerStateAndLegitimateUpdatesStillArrive()throws Exception{
        Account host=account(),second=account(),third=account();int id=lobby(host,second);
        Connection recipient=connect(host),sender=connect(second);var events=subscribe(recipient,host,"/topic/game/"+id);
        sender.session().send("/topic/game/"+id,Map.of("id",id,"probe","forged","players",List.of()));
        assertNotNull(sender.signals().errors.poll(5,TimeUnit.SECONDS));assertNull(events.poll(300,TimeUnit.MILLISECONDS));
        assertEquals(2,count("select count(*) from players where game_id=?",id));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",third.token(),Map.of()));
        JsonNode legitimate=events.poll(5,TimeUnit.SECONDS);assertNotNull(legitimate);assertEquals(id,legitimate.path("id").asInt());assertFalse(legitimate.has("probe"));
    }
    @Test void membershipRevocationStopsExistingDeliveryAndDeletedLobbySendsOnlyDeletion()throws Exception{
        Account host=account(),second=account();int id=lobby(host,second);
        Connection member=connect(second),leader=connect(host);var removed=subscribe(member,second,"/topic/game/"+id);var retained=subscribe(leader,host,"/topic/game/"+id);
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/leave",second.token(),null));
        assertNotNull(retained.poll(5,TimeUnit.SECONDS));assertNull(removed.poll(300,TimeUnit.MILLISECONDS));
        member.session().subscribe("/topic/game/"+id,new Signals());assertNotNull(member.signals().errors.poll(5,TimeUnit.SECONDS));
        Account third=account();status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",third.token(),Map.of()));retained.poll(5,TimeUnit.SECONDS);
        Connection remaining=connect(third);var deleted=subscribe(remaining,third,"/topic/game/"+id);
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/leave",host.token(),null));
        JsonNode signal=deleted.poll(5,TimeUnit.SECONDS);assertNotNull(signal);assertEquals("DELETED",signal.asText());
    }
    @Test void tokenExpiryAlsoStopsDeliveryOnAnAlreadyConnectedSession()throws Exception{
        Account host=account(),second=account();int id=lobby(host,second);
        Date expiry=new Date(System.currentTimeMillis()+12000);
        Connection expiring=connect(token(second,expiry,signingKey)),valid=connect(host);
        var expiredEvents=subscribe(expiring,second,"/topic/game/"+id);var currentEvents=subscribe(valid,host,"/topic/game/"+id);
        publisher.convertAndSend("/topic/game/"+id,Map.of("id",id));
        assertNotNull(expiredEvents.poll(5,TimeUnit.SECONDS));assertNotNull(currentEvents.poll(5,TimeUnit.SECONDS));
        Thread.sleep(Math.max(0,expiry.getTime()-System.currentTimeMillis())+150);
        publisher.convertAndSend("/topic/game/"+id,Map.of("id",id));
        assertNotNull(currentEvents.poll(5,TimeUnit.SECONDS));assertNull(expiredEvents.poll(400,TimeUnit.MILLISECONDS));
        expiring.session().subscribe("/topic/game/"+id,new Signals());assertNotNull(expiring.signals().errors.poll(5,TimeUnit.SECONDS));
    }
}
