package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class GameActionPermissionsTest extends SecurityHttpSupport {
    record Match(int id,int turn,int storyteller,LinkedHashMap<Integer,Account> players) {
        Account narrator(){return players.get(storyteller);}
        List<Integer> voters(){return players.keySet().stream().filter(p->p!=storyteller).toList();}
    }
    Match match() { return match(null); }
    Match match(Account spectator) {
        Account host=account(),second=account(),third=account();
        if (spectator != null) for (Account player:List.of(host,second,third)) {
            status(200,request(HttpMethod.POST,"/friends/addFriend/"+spectator.id(),player.token(),null));
            int friendship=count("select id from friend_requests where sender_id=? and receiver_id=?",player.id(),spectator.id());
            status(200,request(HttpMethod.PATCH,"/friends/"+friendship+"/accept",spectator.token(),null));
        }
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);
        int id=created.getBody().path("id").asInt();
        for(Account p:List.of(second,third)) status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",p.token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/start",host.token(),null));
        int turn=count("select max(id) from turns where game_id=?",id), story=count("select storyteller_id from turns where id=?",turn);
        LinkedHashMap<Integer,Account> players=new LinkedHashMap<>();
        for(Account p:List.of(host,second,third))players.put(count("select id from players where game_id=? and user_id=?",id,p.id()),p);
        return new Match(id,turn,story,players);
    }
    int hand(int player){return count("select min(id) from cards where player_id=? and turn_id is null",player);}
    String path(Match m,String tail){return "/turns/"+m.turn()+tail;}
    String snapshot(Match m){
        List<Object> state=new ArrayList<>();
        state.add(jdbc.queryForList("select * from games where id=?",m.id()));
        state.add(jdbc.queryForList("select * from turns where game_id=? order by id",m.id()));
        state.add(jdbc.queryForList("select * from cards where game_id=? order by id",m.id()));
        state.add(jdbc.queryForList("select * from players where game_id=? order by id",m.id()));
        state.add(jdbc.queryForList("select v.* from player_votes v join cards c on c.id=v.card_id where c.game_id=? order by v.card_id,v.player_id",m.id()));
        return state.toString();
    }
    void denied(int code,Match m,HttpMethod method,String path,Account actor,Object body){
        String before=snapshot(m);
        status(code,request(method,path,actor==null?null:actor.token(),body));
        assertTrue(before.equals(snapshot(m)),"Rejected request must not change game rows");
    }
    int prepare(Match m){
        int answer=hand(m.storyteller());
        status(200,request(HttpMethod.PUT,path(m,"/answer/"+answer),m.narrator().token(),null));
        status(200,request(HttpMethod.PUT,path(m,"/clue"),m.narrator().token(),"fixture clue"));
        return answer;
    }
    Map<Integer,Integer> table(Match m){
        Map<Integer,Integer> cards=new HashMap<>();
        cards.put(m.storyteller(),prepare(m));
        for(int player:m.voters()){
            int card=hand(player);cards.put(player,card);
            status(200,request(HttpMethod.POST,path(m,"/play-card/"+card+"/"+player),m.players().get(player).token(),null));
        }
        return cards;
    }
    @Test void outsidersAndAdminsCannotActAsMembersOrReadTheirGameTurnHandOrChat(){
        Match m=match();int victim=m.voters().getFirst(),answer=hand(m.storyteller());
        for(Account outsider:List.of(account(),admin())){
            denied(403,m,HttpMethod.PUT,path(m,"/answer/"+answer),outsider,null);
            denied(403,m,HttpMethod.PUT,path(m,"/clue"),outsider,"clue");
            denied(403,m,HttpMethod.POST,path(m,"/play-card/"+hand(victim)+"/"+victim),outsider,null);
            denied(403,m,HttpMethod.POST,path(m,"/vote/"+victim+"/"+answer),outsider,null);
            denied(403,m,HttpMethod.GET,"/games/"+m.id(),outsider,null);
            denied(403,m,HttpMethod.GET,"/turns/"+m.turn(),outsider,null);
            denied(403,m,HttpMethod.GET,"/players/"+victim,outsider,null);
            denied(403,m,HttpMethod.GET,"/players/"+victim+"/cards",outsider,null);
            denied(403,m,HttpMethod.GET,"/messages/games/"+m.id(),outsider,null);
        }
        denied(401,m,HttpMethod.GET,"/turns/"+m.turn(),null,null);
        denied(401,m,HttpMethod.PUT,path(m,"/answer/"+answer),null,null);
        status(200,request(HttpMethod.GET,"/players/"+victim+"/cards",m.players().get(victim).token(),null));
    }
    @Test void spectatorMayReadButCannotActOrSelectAForeignPerspective(){
        Account viewer=account();Match m=match(viewer);int player=m.voters().getFirst();
        status(200,request(HttpMethod.GET,"/games/"+m.id(),viewer.token(),null));
        status(200,request(HttpMethod.GET,"/turns/"+m.turn(),viewer.token(),null));
        status(200,request(HttpMethod.PATCH,"/games/"+m.id()+"/spectate/"+player,viewer.token(),null));
        denied(400,m,HttpMethod.PATCH,"/games/"+m.id()+"/spectate/99999999",viewer,null);
        denied(403,m,HttpMethod.PUT,path(m,"/answer/"+hand(m.storyteller())),viewer,null);
        denied(403,m,HttpMethod.POST,path(m,"/play-card/"+hand(player)+"/"+player),viewer,null);
        denied(403,m,HttpMethod.POST,path(m,"/vote/"+player+"/"+hand(m.storyteller())),viewer,null);
    }
    @Test void sameUserCannotPlayTheirCardFromAnotherGame(){
        Match m=match();Account actor=m.narrator();
        var created=request(HttpMethod.POST,"/games",actor.token(),Map.of());status(201,created);int other=created.getBody().path("id").asInt();
        for (int player:m.voters()) status(200,request(HttpMethod.PATCH,"/games/"+other+"/join",m.players().get(player).token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+other+"/start",actor.token(),null));
        int foreign=count("select id from players where game_id=? and user_id=?",other,actor.id());
        denied(403,m,HttpMethod.PUT,path(m,"/answer/"+hand(foreign)),actor,null);
        assertEquals(0,count("select count(*) from cards where game_id=? and turn_id is not null",other));
    }
    @Test void validatesNarratorOwnershipAndSelectionPhasesBeforeMutation(){
        Match m=match();int p=m.voters().getFirst();Account voter=m.players().get(p);
        denied(403,m,HttpMethod.PUT,path(m,"/answer/"+hand(p)),voter,null);
        denied(403,m,HttpMethod.PUT,path(m,"/answer/"+hand(p)),m.narrator(),null);
        denied(400,m,HttpMethod.PUT,path(m,"/clue"),m.narrator(),"too early");
        denied(400,m,HttpMethod.POST,path(m,"/play-card/"+hand(p)+"/"+p),voter,null);
        denied(400,m,HttpMethod.POST,path(m,"/vote/"+p+"/"+hand(m.storyteller())),voter,null);
        int answer=hand(m.storyteller());
        status(200,request(HttpMethod.PUT,path(m,"/answer/"+answer),m.narrator().token(),null));
        denied(400,m,HttpMethod.PUT,path(m,"/answer/"+hand(m.storyteller())),m.narrator(),null);
        denied(403,m,HttpMethod.PUT,path(m,"/clue"),voter,"foreign clue");
        denied(400,m,HttpMethod.PUT,path(m,"/clue"),m.narrator(),"  ");
        status(200,request(HttpMethod.PUT,path(m,"/clue"),m.narrator().token(),"valid clue"));
    }
    @Test void validRoundRejectsImpersonationOwnVoteNarratorDuplicateAndOldTurn(){
        Match m=match();int answer=prepare(m),p=m.voters().getFirst(),q=m.voters().getLast(),card=hand(p);
        Account a=m.players().get(p),b=m.players().get(q);
        denied(403,m,HttpMethod.POST,path(m,"/play-card/"+hand(m.storyteller())+"/"+m.storyteller()),m.narrator(),null);
        denied(403,m,HttpMethod.POST,path(m,"/play-card/"+hand(q)+"/"+q),a,null);
        denied(403,m,HttpMethod.POST,path(m,"/play-card/"+hand(q)+"/"+p),a,null);
        status(200,request(HttpMethod.POST,path(m,"/play-card/"+card+"/"+p),a.token(),null));
        denied(400,m,HttpMethod.POST,path(m,"/play-card/"+hand(p)+"/"+p),a,null);
        status(200,request(HttpMethod.POST,path(m,"/play-card/"+hand(q)+"/"+q),b.token(),null));
        denied(403,m,HttpMethod.POST,path(m,"/vote/"+p+"/"+card),a,null);
        denied(403,m,HttpMethod.POST,path(m,"/vote/"+m.storyteller()+"/"+card),m.narrator(),null);
        denied(403,m,HttpMethod.POST,path(m,"/vote/"+q+"/"+answer),a,null);
        denied(400,m,HttpMethod.POST,path(m,"/vote/"+p+"/"+hand(p)),a,null);
        status(200,request(HttpMethod.POST,path(m,"/vote/"+p+"/"+answer),a.token(),null));
        denied(400,m,HttpMethod.POST,path(m,"/vote/"+p+"/"+answer),a,null);
        status(200,request(HttpMethod.POST,path(m,"/vote/"+q+"/"+answer),b.token(),null));
        assertEquals(2,count("select count(*) from turns where game_id=?",m.id()));
        assertEquals("FINISHED",jdbc.queryForObject("select state from turns where id=?",String.class,m.turn()));
        denied(400,m,HttpMethod.POST,path(m,"/vote/"+q+"/"+answer),b,null);
        denied(400,m,HttpMethod.PUT,path(m,"/answer/"+answer),m.narrator(),null);
        denied(400,m,HttpMethod.PUT,path(m,"/clue"),m.narrator(),"late clue");
        denied(400,m,HttpMethod.POST,path(m,"/play-card/"+hand(p)+"/"+p),a,null);
    }
    @Test void duplicateConcurrentPlaysAndVotesHaveOnlyOneEffect() throws Exception{
        Match m=match();int answer=prepare(m),p=m.voters().getFirst(),q=m.voters().getLast(),card=hand(p);
        Account actor=m.players().get(p);
        assertEquals(List.of(200,400),race(()->request(HttpMethod.POST,path(m,"/play-card/"+card+"/"+p),actor.token(),null).getStatusCode().value()));
        status(200,request(HttpMethod.POST,path(m,"/play-card/"+hand(q)+"/"+q),m.players().get(q).token(),null));
        assertEquals(List.of(200,400),race(()->request(HttpMethod.POST,path(m,"/vote/"+p+"/"+answer),actor.token(),null).getStatusCode().value()));
        assertEquals(1,count("select count(*) from player_votes v join cards c on c.id=v.card_id where c.turn_id=?",m.turn()));
        assertEquals(1,count("select count(*) from turns where game_id=?",m.id()));
        status(200,request(HttpMethod.POST,path(m,"/vote/"+q+"/"+answer),m.players().get(q).token(),null));
        assertEquals(2,count("select count(*) from turns where game_id=?",m.id()));
    }
    List<Integer> race(Callable<Integer> operation)throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        try{List<Future<Integer>> jobs=List.of(pool.submit(()->{gate.await();return operation.call();}),pool.submit(()->{gate.await();return operation.call();}));gate.countDown();List<Integer> result=new ArrayList<>();for(var job:jobs)result.add(job.get(10,TimeUnit.SECONDS));return result.stream().sorted().toList();}
        finally{pool.shutdownNow();}
    }
    @Test void genericUpdateAndLobbyLeaveCannotBypassInitializationOrDestroyActiveHistory(){
        Account host=account(),other=account();var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);int id=created.getBody().path("id").asInt();
        status(403,request(HttpMethod.PUT,"/games/"+id,other.token(),Map.of("password","forged")));
        status(400,request(HttpMethod.PUT,"/games/"+id,host.token(),Map.of("startDate","2026-01-01","players",List.of())));
        assertEquals(1,count("select count(*) from players where game_id=?",id));
        assertEquals(0,count("select count(*) from turns where game_id=?",id));
        status(200,request(HttpMethod.PUT,"/games/"+id,host.token(),Map.of("password","new-fixture")));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/join",other.token(),Map.of("password","new-fixture")));
        status(200,request(HttpMethod.PATCH,"/games/"+id+"/leave",other.token(),null));
        Match m=match();Account leader=m.players().values().iterator().next();
        denied(400,m,HttpMethod.PATCH,"/games/"+m.id()+"/leave",leader,null);
        denied(403,m,HttpMethod.PATCH,"/games/"+m.id()+"/leave",other,null);
        denied(400,m,HttpMethod.PUT,"/games/"+m.id(),leader,Map.of("password","late"));
    }
    @Test void invitationRequiresLobbyMembershipAndFriendshipAndKeepsRecipientChecks(){
        Account host=account(),invitee=account(),outsider=account();
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);int id=created.getBody().path("id").asInt();
        int before=count("select count(*) from game_invitations");
        status(403,request(HttpMethod.POST,"/games/"+id+"/invite",outsider.token(),Map.of("invited",String.valueOf(invitee.id()))));
        status(403,request(HttpMethod.POST,"/games/"+id+"/invite",host.token(),Map.of("invited",String.valueOf(invitee.id()))));
        assertEquals(before,count("select count(*) from game_invitations"));
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+invitee.id(),host.token(),null));
        int friendship=count("select id from friend_requests where sender_id=? and receiver_id=?",host.id(),invitee.id());
        status(200,request(HttpMethod.PATCH,"/friends/"+friendship+"/accept",invitee.token(),null));
        status(200,request(HttpMethod.POST,"/games/"+id+"/invite",host.token(),Map.of("invited",String.valueOf(invitee.id()))));
        int invitation=count("select id from game_invitations where game_id=? and invited_id=?",id,invitee.id());
        status(400,request(HttpMethod.POST,"/games/"+id+"/invite",host.token(),Map.of("invited",String.valueOf(invitee.id()))));
        status(403,request(HttpMethod.PATCH,"/games/invitations/"+invitation+"/accept",outsider.token(),null));
        assertEquals(1,count("select count(*) from game_invitations where id=?",invitation));
        Match m=match();int invitations=count("select count(*) from game_invitations");
        status(400,request(HttpMethod.POST,"/games/"+m.id()+"/invite",m.players().values().iterator().next().token(),Map.of("invited",String.valueOf(invitee.id()))));
        assertEquals(invitations,count("select count(*) from game_invitations"));
    }
}
