package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GamePhasePrivacyTest extends SecurityHttpSupport {
    record Match(int id,LinkedHashMap<Integer,Account> members,Account host,Account viewer) {}
    Match lobbyWithViewer(){
        List<Account> people=List.of(account(),account(),account());Account viewer=account();
        for(Account person:people){
            status(200,request(HttpMethod.POST,"/friends/addFriend/"+viewer.id(),person.token(),null));
            int friendship=count("select id from friend_requests where sender_id=? and receiver_id=?",person.id(),viewer.id());
            status(200,request(HttpMethod.PATCH,"/friends/"+friendship+"/accept",viewer.token(),null));
        }
        var created=request(HttpMethod.POST,"/games",people.getFirst().token(),Map.of("password","private-lobby-fixture"));status(201,created);safe(created.getBody());
        int id=created.getBody().path("id").asInt();
        for(Account person:people.subList(1,people.size())){
            var joined=request(HttpMethod.PATCH,"/games/"+id+"/join",person.token(),Map.of("password","private-lobby-fixture"));status(200,joined);safe(joined.getBody());
        }
        LinkedHashMap<Integer,Account> members=new LinkedHashMap<>();
        created=request(HttpMethod.GET,"/games/"+id,people.getFirst().token(),null);status(200,created);
        for(JsonNode participant:created.getBody().path("players")){
            Account user=people.stream().filter(p->p.id()==participant.path("user").path("id").asInt()).findFirst().orElseThrow();
            members.put(participant.path("id").asInt(),user);
        }
        Account host=people.getFirst();
        assertEquals(host.id(),created.getBody().path("leader").path("id").asInt());
        // Deliberately place the host last: SQL/JPA collection order is not actor identity.
        int hostPlayer=members.entrySet().stream().filter(e->e.getValue().id()==host.id()).findFirst().orElseThrow().getKey();
        members.remove(hostPlayer);members.put(hostPlayer,host);
        assertNotEquals(host.id(),members.values().iterator().next().id());
        return new Match(id,members,host,viewer);
    }
    void start(Match m){var r=request(HttpMethod.PATCH,"/games/"+m.id()+"/start",m.host().token(),null);status(200,r);safe(r.getBody());}
    JsonNode game(Match m,Account user,Integer perspective){
        var r=request(HttpMethod.GET,"/games/"+m.id()+(perspective==null?"":"?perspectivePlayerId="+perspective),user.token(),null);status(200,r);safe(r.getBody());return r.getBody();
    }
    JsonNode round(Match m,int turn,Account user,Integer perspective){
        var r=request(HttpMethod.GET,"/turns/"+turn+(perspective==null?"":"?perspectivePlayerId="+perspective),user.token(),null);status(200,r);safe(r.getBody());return r.getBody();
    }
    void safe(JsonNode node){
        if(node.isObject()){
            for(String field:List.of("password","deck","tableOrderSeed","gamesToSpectate","authority","achievements"))assertFalse(node.has(field),"Forbidden field "+field);
            node.elements().forEachRemaining(this::safe);
        }else if(node.isArray())node.forEach(this::safe);
        else if(node.isTextual())assertFalse(node.asText().contains("$2a$")||node.asText().contains("$2b$"),"No password hash in wire view");
    }
    JsonNode participant(JsonNode game,int id){for(JsonNode player:game.path("players"))if(player.path("id").asInt()==id)return player;throw new AssertionError("Missing participant");}
    void hands(JsonNode game,Integer perspective,int expected){
        for(JsonNode player:game.path("players")){
            assertEquals(expected,player.path("handSize").asInt());
            assertEquals(Objects.equals(perspective,player.path("id").asInt())?expected:0,player.path("cards").size());
        }
    }
    void anonymous(JsonNode turn,boolean knowsAnswer){
        assertEquals(knowsAnswer,turn.hasNonNull("answer"));
        for(JsonNode card:turn.path("playedCards")){
            assertEquals(Set.of("id","image","isOwn"),fields(card));
            assertFalse(card.has("owner"));assertFalse(card.has("user"));assertFalse(card.has("votes"));
        }
    }
    Set<String> fields(JsonNode node){Set<String> names=new HashSet<>();node.fieldNames().forEachRemaining(names::add);return names;}
    List<Integer> cardIds(JsonNode turn){List<Integer> ids=new ArrayList<>();turn.path("playedCards").forEach(c->ids.add(c.path("id").asInt()));return ids;}
    String history(int game){return jdbc.queryForList("select c.id,c.image_id,c.turn_id,c.user_id,c.player_id,v.player_id as voter from cards c left join player_votes v on c.id=v.card_id where c.game_id=? and c.turn_id is not null order by c.id,v.player_id",game).toString();}

    @Test void initialViewsProtectHandsProfilesLobbySecretAndAdministrativeCatalogs(){
        Match m=lobbyWithViewer();Account host=m.host();
        int hostPlayer=m.members().entrySet().stream().filter(e->e.getValue().id()==host.id()).findFirst().orElseThrow().getKey();
        JsonNode lobby=game(m,host,null);hands(lobby,hostPlayer,0);assertTrue(lobby.path("hasPassword").asBoolean());
        start(m);
        for(var member:m.members().entrySet())hands(game(m,member.getValue(),null),member.getKey(),6);
        hands(game(m,m.viewer(),null),null,6);
        for(int perspective:m.members().keySet()){
            hands(game(m,m.viewer(),perspective),perspective,6);
            status(200,request(HttpMethod.GET,"/players/"+perspective+"/cards?perspectivePlayerId="+perspective,m.viewer().token(),null));
        }
        Account first=m.members().values().iterator().next();int rival=m.members().keySet().stream().skip(1).findFirst().orElseThrow();
        var player=request(HttpMethod.GET,"/players/"+rival,first.token(),null);status(200,player);assertEquals(6,player.getBody().path("handSize").asInt());assertEquals(0,player.getBody().path("cards").size());safe(player.getBody());
        status(403,request(HttpMethod.GET,"/players/"+rival+"/cards",first.token(),null));
        status(403,request(HttpMethod.GET,"/games/"+m.id()+"?perspectivePlayerId="+rival,first.token(),null));
        status(400,request(HttpMethod.GET,"/games/"+m.id()+"?perspectivePlayerId=999999",m.viewer().token(),null));
        Account admin=admin(),outsider=account();
        status(403,request(HttpMethod.GET,"/games/"+m.id(),admin.token(),null));
        status(403,request(HttpMethod.GET,"/games/"+m.id(),outsider.token(),null));
        status(403,request(HttpMethod.GET,"/games/inProgress",outsider.token(),null));
        var all=request(HttpMethod.GET,"/games",admin.token(),null);status(200,all);safe(all.getBody());
        for(JsonNode catalog:all.getBody()){assertFalse(catalog.has("turns"));for(JsonNode p:catalog.path("players")){assertFalse(p.has("cards"));assertFalse(p.has("playerVote"));}}
        var mine=request(HttpMethod.GET,"/games/my",host.token(),null);status(200,mine);safe(mine.getBody());
        mine.getBody().forEach(g->assertFalse(g.has("turns")));
    }
    @Test void responseClueTableVotesAndHistoryHaveExactlyTheApprovedVisibility(){
        Match m=lobbyWithViewer();start(m);
        JsonNode initial=game(m,m.members().values().iterator().next(),null);JsonNode turn=initial.path("turns").get(0);
        int tid=turn.path("id").asInt(),story=turn.path("storyteller").path("id").asInt();Account narrator=m.members().get(story);
        String seed=jdbc.queryForObject("select table_order_seed from turns where id=?",String.class,tid);assertNotNull(seed);
        Map<Integer,Integer> choices=new HashMap<>();
        for(var entry:m.members().entrySet()){
            JsonNode own=game(m,entry.getValue(),null);choices.put(entry.getKey(),participant(own,entry.getKey()).path("cards").get(0).path("id").asInt());
            assertFalse(round(m,tid,entry.getValue(),null).hasNonNull("answer"));
        }
        var chosen=request(HttpMethod.PUT,"/turns/"+tid+"/answer/"+choices.get(story),narrator.token(),null);status(200,chosen);safe(chosen.getBody());anonymous(chosen.getBody(),true);
        for(var member:m.members().entrySet()){
            JsonNode own=round(m,tid,member.getValue(),null);anonymous(own,member.getKey()==story);
            assertEquals(member.getKey()==story?1:0,own.path("playedCards").size());
        }
        anonymous(round(m,tid,m.viewer(),story),true);
        status(200,request(HttpMethod.PUT,"/turns/"+tid+"/clue",narrator.token(),"fixture clue"));
        List<Integer> voters=m.members().keySet().stream().filter(p->p!=story).toList();
        int first=voters.getFirst(),last=voters.getLast();
        var played=request(HttpMethod.POST,"/turns/"+tid+"/play-card/"+choices.get(first)+"/"+first,m.members().get(first).token(),null);status(200,played);safe(played.getBody());anonymous(played.getBody(),false);
        assertTrue(played.getBody().path("hasPlayed").asBoolean());assertEquals(1,played.getBody().path("playedCards").size());
        assertEquals(1,round(m,tid,narrator,null).path("playedCards").size());
        assertEquals(0,round(m,tid,m.members().get(last),null).path("playedCards").size());
        status(200,request(HttpMethod.POST,"/turns/"+tid+"/play-card/"+choices.get(last)+"/"+last,m.members().get(last).token(),null));
        List<Integer> order=cardIds(round(m,tid,m.members().get(first),null));assertEquals(3,order.size());
        for(var member:m.members().entrySet()){
            JsonNode table=round(m,tid,member.getValue(),null);assertEquals(order,cardIds(table));anonymous(table,member.getKey()==story);
            assertEquals(1,table.path("playedCards").findValues("isOwn").stream().filter(JsonNode::asBoolean).count());
            hands(game(m,member.getValue(),null),member.getKey(),5);
        }
        assertEquals(order,cardIds(round(m,tid,m.viewer(),first)));assertEquals(order,cardIds(round(m,tid,m.viewer(),story)));
        status(200,request(HttpMethod.POST,"/turns/"+tid+"/vote/"+first+"/"+choices.get(story),m.members().get(first).token(),null));
        assertTrue(round(m,tid,m.members().get(first),null).path("hasVoted").asBoolean());assertFalse(round(m,tid,m.members().get(last),null).path("hasVoted").asBoolean());
        // A fresh REST read recovers only this perspective's persisted choice.
        assertEquals(choices.get(story).intValue(),round(m,tid,m.members().get(first),null).path("ownVotedCardId").asInt());
        assertEquals(choices.get(story).intValue(),game(m,m.members().get(first),null).path("turns").get(0).path("ownVotedCardId").asInt());
        for(Account other:List.of(m.members().get(last),narrator,m.viewer()))assertFalse(round(m,tid,other,null).hasNonNull("ownVotedCardId"));
        assertEquals(choices.get(story).intValue(),round(m,tid,m.viewer(),first).path("ownVotedCardId").asInt());
        assertFalse(round(m,tid,m.viewer(),last).hasNonNull("ownVotedCardId"));
        assertFalse(round(m,tid,m.viewer(),story).hasNonNull("ownVotedCardId"));
        status(400,request(HttpMethod.POST,"/turns/"+tid+"/vote/"+first+"/"+choices.get(last),m.members().get(first).token(),null));
        assertEquals(choices.get(story).intValue(),round(m,tid,m.members().get(first),null).path("ownVotedCardId").asInt());
        for(var member:m.members().entrySet())anonymous(round(m,tid,member.getValue(),null),member.getKey()==story);
        var closed=request(HttpMethod.POST,"/turns/"+tid+"/vote/"+last+"/"+choices.get(story),m.members().get(last).token(),null);status(200,closed);safe(closed.getBody());
        assertEquals("FINISHED",closed.getBody().path("state").asText());
        String before=history(m.id());
        List<Account> readers=new ArrayList<>(m.members().values()); readers.add(m.viewer());
        for(Account user:readers){
            JsonNode history=round(m,tid,user,null);assertTrue(history.hasNonNull("answer"));assertEquals(order,cardIds(history));
            int votes=0;for(JsonNode card:history.path("playedCards")){assertTrue(card.has("user"));assertTrue(card.has("votes"));votes+=card.path("votes").size();}
            assertEquals(2,votes);
        }
        for(var member:m.members().entrySet())hands(game(m,member.getValue(),null),member.getKey(),6);
        assertTrue(before.equals(history(m.id())),"Projection must not change historical cards, authors or votes");
        assertTrue(seed.equals(jdbc.queryForObject("select table_order_seed from turns where id=?",String.class,tid)));
    }
    @Test void spectatorKeepsAuthorizedPerspectiveThroughFinalizationWithoutRevealingOtherHands(){
        Match m=lobbyWithViewer();start(m);
        for(int index=0;index<6;index++){
            JsonNode state=game(m,m.members().values().iterator().next(),null);JsonNode turn=state.path("turns").get(index);
            int tid=turn.path("id").asInt(),story=turn.path("storyteller").path("id").asInt();Map<Integer,Integer> choices=new HashMap<>();
            for(var member:m.members().entrySet())choices.put(member.getKey(),participant(game(m,member.getValue(),null),member.getKey()).path("cards").get(0).path("id").asInt());
            status(200,request(HttpMethod.PUT,"/turns/"+tid+"/answer/"+choices.get(story),m.members().get(story).token(),null));
            status(200,request(HttpMethod.PUT,"/turns/"+tid+"/clue",m.members().get(story).token(),"round "+index));
            for(var member:m.members().entrySet())if(member.getKey()!=story)status(200,request(HttpMethod.POST,"/turns/"+tid+"/play-card/"+choices.get(member.getKey())+"/"+member.getKey(),member.getValue().token(),null));
            for(var member:m.members().entrySet())if(member.getKey()!=story)status(200,request(HttpMethod.POST,"/turns/"+tid+"/vote/"+member.getKey()+"/"+choices.get(story),member.getValue().token(),null));
            for(int perspective:m.members().keySet())hands(game(m,m.viewer(),perspective),perspective,index==5?5:6);
        }
        for(int player:m.members().keySet())assertEquals(2,count("select count(*) from turns where game_id=? and storyteller_id=? and state='FINISHED'",m.id(),player));
        JsonNode finalView=game(m,m.viewer(),m.members().keySet().iterator().next());assertFalse(finalView.path("endDate").isNull());assertFalse(finalView.path("winners").isEmpty());
        assertEquals(6,finalView.path("turns").size());assertEquals(18,count("select count(*) from cards where game_id=? and turn_id is not null and user_id is not null",m.id()));
    }
}
