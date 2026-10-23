package es.us.dp1.lx_xy_24_25.dixit.game;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

/** Single-process presence for the existing in-memory broker; never changes a participant seat. */
@Component
public class GamePresence {
    private final GameService games;
    private final long graceMillis;
    private final Map<String,Map<String,Integer>> hostSubscriptions=new HashMap<>();
    private record Deadline(Object generation,ScheduledFuture<?> future) {}
    private final Map<Integer,Deadline> deadlines=new HashMap<>();
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(task->{
        Thread thread=new Thread(task,"host-disconnect-grace");thread.setDaemon(true);return thread;
    });
    public GamePresence(GameService games,@Value("${dixit.host-disconnect-grace-ms:30000}") long graceMillis){
        this.games=games;this.graceMillis=graceMillis;
    }
    public void subscribed(String session,String subscription,Integer gameId,String username){
        if(!games.isHost(gameId,username))return;
        synchronized(this){
            hostSubscriptions.computeIfAbsent(session,key->new HashMap<>()).put(subscription,gameId);
            Deadline pending=deadlines.remove(gameId);if(pending!=null)pending.future().cancel(false);
        }
    }
    public synchronized void unsubscribed(String session,String subscription){
        Map<String,Integer> subscriptions=hostSubscriptions.get(session);
        if(subscriptions==null)return;
        Integer gameId=subscriptions.remove(subscription);
        if(subscriptions.isEmpty())hostSubscriptions.remove(session);
        if(gameId!=null)lostLastConnection(gameId);
    }
    public synchronized void disconnected(String session){
        Map<String,Integer> subscriptions=hostSubscriptions.remove(session);
        if(subscriptions!=null)new HashSet<>(subscriptions.values()).forEach(this::lostLastConnection);
    }
    private boolean connected(Integer id){return hostSubscriptions.values().stream().anyMatch(subs->subs.containsValue(id));}
    private void lostLastConnection(Integer gameId){
        if(connected(gameId)||deadlines.containsKey(gameId)||!games.isRunning(gameId))return;
        Object generation=new Object();
        deadlines.put(gameId,new Deadline(generation,timer.schedule(()->expire(gameId,generation),graceMillis,TimeUnit.MILLISECONDS)));
    }
    private synchronized void expire(Integer gameId,Object generation){
        Deadline deadline=deadlines.get(gameId);
        if(deadline==null || deadline.generation()!=generation || connected(gameId))return;
        try{games.cancelForHostDisconnect(gameId);deadlines.remove(gameId);}
        catch(RuntimeException failure){
            org.slf4j.LoggerFactory.getLogger(GamePresence.class).error("Host disconnection cancellation failed for game {}; retrying",gameId,failure);
            deadlines.put(gameId,new Deadline(generation,timer.schedule(()->expire(gameId,generation),1000,TimeUnit.MILLISECONDS)));
        }
    }
    @PreDestroy public void stop(){timer.shutdownNow();}
}
