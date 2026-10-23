package es.us.dp1.lx_xy_24_25.dixit.game;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class GamePresenceTest {
    @Test void failedExpiryIsRetriedWithoutAnotherDisconnect()throws Exception{
        GameService games=mock(GameService.class);when(games.isHost(42,"host")).thenReturn(true);when(games.isRunning(42)).thenReturn(true);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture only")).doNothing().when(games).cancelForHostDisconnect(42);
        GamePresence presence=new GamePresence(games,10);
        try{
            presence.subscribed("session","game",42,"host");presence.disconnected("session");
            verify(games,timeout(2500).times(2)).cancelForHostDisconnect(42);
        }finally{presence.stop();}
    }
}
