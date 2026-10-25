package es.us.dp1.lx_xy_24_25.dixit.presence;

import org.springframework.stereotype.Component;
import java.util.*;

/** Confirmed authenticated transports only; never changes a game subscription or seat. */
@Component
public class AccountPresence {
    private final Map<String,Integer> connections = new HashMap<>();
    public synchronized void connected(String sessionId, Integer accountId) {connections.put(sessionId,accountId);}
    public synchronized void disconnected(String sessionId) {connections.remove(sessionId);}
    public synchronized Set<Integer> onlineAccounts() {return Set.copyOf(connections.values());}
}
