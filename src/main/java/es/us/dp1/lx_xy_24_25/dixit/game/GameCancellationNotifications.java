package es.us.dp1.lx_xy_24_25.dixit.game;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class GameCancellationNotifications {
    private final SimpMessagingTemplate notifications;
    public GameCancellationNotifications(SimpMessagingTemplate notifications){this.notifications=notifications;}
    @TransactionalEventListener
    public void cancelled(GameCancelled event){notifications.convertAndSend("/topic/game/"+event.gameId(),GameEvent.changed(event.gameId()));}
}
