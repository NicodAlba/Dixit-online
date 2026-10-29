package es.us.dp1.lx_xy_24_25.dixit.presence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
class AccountPresenceTest {
    @Test void anAccountRemainsOnlineUntilItsLastDistinctConnectionCloses() {
        var presence=new AccountPresence();
        presence.connected("tab-one",71);presence.connected("tab-one",71);presence.connected("tab-two",71);
        assertEquals(Set.of(71),presence.onlineAccounts());
        presence.disconnected("tab-one");presence.disconnected("tab-one");
        assertEquals(Set.of(71),presence.onlineAccounts());
        presence.disconnected("tab-two");assertTrue(presence.onlineAccounts().isEmpty());
    }
}
