package es.us.dp1.lx_xy_24_25.dixit.configuration;

import es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.JwtUtils;
import es.us.dp1.lx_xy_24_25.dixit.configuration.services.UserDetailsServiceImpl;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.game.TurnRepository;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** JWT remains private to this connection; HTTP is the only action transport. */
@Component
public class StompAccess {
    private record Session(String token, String username, Map<String,String> subscriptions) {}
    private static final Pattern TOPIC = Pattern.compile("/topic/(game|turn)/([1-9][0-9]*)");
    private final Map<String,Session> sessions = new ConcurrentHashMap<>();
    private final JwtUtils jwt;
    private final UserDetailsServiceImpl users;
    private final UserService accounts;
    private final GameService games;
    private final TurnRepository turns;

    public StompAccess(JwtUtils jwt, UserDetailsServiceImpl users, UserService accounts, GameService games, TurnRepository turns) {
        this.jwt=jwt;this.users=users;this.accounts=accounts;this.games=games;this.turns=turns;
    }
    public ChannelInterceptor inbound() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor headers=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
                if(headers==null) throw new AccessDeniedException("Invalid STOMP frame.");
                String id=headers.getSessionId();
                if(headers.getCommand()==StompCommand.CONNECT) {
                    String authorization=headers.getFirstNativeHeader("Authorization");
                    if(authorization==null) authorization=headers.getFirstNativeHeader("authorization");
                    if(id==null || authorization==null || !authorization.startsWith("Bearer ")) throw new AccessDeniedException("STOMP authentication required.");
                    String token=authorization.substring(7);
                    if(!jwt.validateJwtToken(token)) throw new AccessDeniedException("Invalid STOMP authentication.");
                    String username=jwt.getUserNameFromJwtToken(token);
                    var details=users.loadUserByUsername(username);
                    headers.setUser(new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities()));
                    sessions.put(id,new Session(token,username,new ConcurrentHashMap<>()));
                    return message;
                }
                if(headers.getCommand()==StompCommand.DISCONNECT) return message;
                Session session=authenticated(id);
                if(headers.getCommand()==StompCommand.SEND) throw new AccessDeniedException("Game actions use REST; clients cannot publish server state.");
                if(headers.getCommand()==StompCommand.SUBSCRIBE) {
                    if(headers.getSubscriptionId()==null) throw new AccessDeniedException("Subscription id required.");
                    authorize(session,headers.getDestination());
                    session.subscriptions().put(headers.getSubscriptionId(),headers.getDestination());
                }
                if(headers.getCommand()==StompCommand.UNSUBSCRIBE) session.subscriptions().remove(headers.getSubscriptionId());
                return message;
            }
        };
    }
    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if(SimpMessageHeaderAccessor.getMessageType(message.getHeaders())!=SimpMessageType.MESSAGE) return message;
                String id=SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                String subscription=SimpMessageHeaderAccessor.getSubscriptionId(message.getHeaders());
                String destination=SimpMessageHeaderAccessor.getDestination(message.getHeaders());
                try {
                    Session session=authenticated(id);
                    if(subscription==null || destination==null || !destination.equals(session.subscriptions().get(subscription))) return null;
                    try { authorize(session,destination); }
                    catch(ResourceNotFoundException absent) {
                        // A deleted lobby has no membership left to query. Only its minimal deletion
                        // signal may reach a previously authorized subscriber, never an entity payload.
                        if(!destination.startsWith("/topic/game/") || !isDeletion(message.getPayload())) return null;
                    }
                    return message;
                } catch(RuntimeException denied) { return null; }
            }
        };
    }
    private Session authenticated(String id) {
        Session session=id==null?null:sessions.get(id);
        if(session==null || !jwt.validateJwtToken(session.token())) throw new AccessDeniedException("STOMP authentication expired or missing.");
        // Recheck account existence/authority from the database, not client JWT role claims.
        users.loadUserByUsername(session.username());
        return session;
    }
    private void authorize(Session session,String destination) {
        var match=TOPIC.matcher(destination==null?"":destination);
        if(!match.matches()) throw new AccessDeniedException("Subscription destination is not allowed.");
        Integer id;
        try { id=Integer.valueOf(match.group(2)); }
        catch(NumberFormatException invalid) { throw new AccessDeniedException("Invalid subscription destination."); }
        Integer gameId=match.group(1).equals("game")?id:turns.findGameIdById(id).orElseThrow(() -> new ResourceNotFoundException("Turn", "id", id));
        games.findByIdIfUserInGame(gameId,accounts.findByUsername(session.username()));
    }
    private boolean isDeletion(Object payload) {
        String text=payload instanceof byte[] bytes?new String(bytes,StandardCharsets.UTF_8):String.valueOf(payload);
        return "DELETED".equals(text)||"\"DELETED\"".equals(text);
    }
    @EventListener public void disconnected(SessionDisconnectEvent event) { sessions.remove(event.getSessionId()); }
}
