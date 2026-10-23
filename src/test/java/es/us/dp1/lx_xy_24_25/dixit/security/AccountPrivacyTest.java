package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountPrivacyTest extends SecurityHttpSupport {
    private void noAccountSecrets(JsonNode node) {
        assertNotNull(node);
        if(node.isObject()) {
            if(node.has("username")) {
                assertFalse(node.has("password"));
                assertFalse(node.has("players"));
                assertFalse(node.has("gamesToSpectate"));
            }
            node.elements().forEachRemaining(this::noAccountSecrets);
        } else if(node.isArray()) node.forEach(this::noAccountSecrets);
        else if(node.isTextual()) assertFalse(node.asText().matches(".*\\$2[aby]\\$[0-9]{2}\\$.*"),"No BCrypt hash in response");
    }
    @Test void readsRequireSessionAndAdminListButKeepSafeNameLookupAndProfile() {
        Account owner=account(), other=account(), admin=admin();
        status(401,request(HttpMethod.GET,"/users",null,null));
        status(401,request(HttpMethod.GET,"/users/"+owner.id(),null,null));
        status(403,request(HttpMethod.GET,"/users",owner.token(),null));
        status(403,request(HttpMethod.GET,"/users/authorities",owner.token(),null));
        var summary=request(HttpMethod.GET,"/users/"+owner.id(),other.token(),null);
        status(200,summary);noAccountSecrets(summary.getBody());
        assertEquals(Set.of("id","username"),fields(summary.getBody()));
        var own=request(HttpMethod.GET,"/users/"+owner.id(),owner.token(),null);
        status(200,own);noAccountSecrets(own.getBody());assertEquals("PLAYER",own.getBody().path("authority").path("authority").asText());
        var list=request(HttpMethod.GET,"/users",admin.token(),null);
        status(200,list);noAccountSecrets(list.getBody());
        assertTrue(list.getBody().findValues("authority").stream().anyMatch(a -> "ADMIN".equals(a.path("authority").asText())));
        var search=request(HttpMethod.GET,"/users/players?username="+owner.name(),other.token(),null);
        status(200,search);noAccountSecrets(search.getBody());
        assertEquals(owner.id(),search.getBody().path("content").get(0).path("id").asInt());
        status(400,request(HttpMethod.GET,"/users/players",owner.token(),null));
    }
    @Test void safeSerializationCoversNestedGamesPlayersFriendsAchievementsAndWrites() {
        Account host=account(), guest=account(), third=account(), admin=admin();
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of("password","fixture-lobby"));
        status(201,created);noAccountSecrets(created.getBody());int game=created.getBody().path("id").asInt();
        for(Account user:List.of(guest,third)) {
            var joined=request(HttpMethod.PATCH,"/games/"+game+"/join",user.token(),Map.of("password","fixture-lobby"));
            status(200,joined);noAccountSecrets(joined.getBody());
        }
        var started=request(HttpMethod.PATCH,"/games/"+game+"/start",host.token(),null);
        status(200,started);noAccountSecrets(started.getBody());
        noAccountSecrets(request(HttpMethod.GET,"/games/"+game,host.token(),null).getBody());
        int pid=count("select id from players where game_id=? and user_id=?",game,host.id());
        noAccountSecrets(request(HttpMethod.GET,"/players/"+pid,host.token(),null).getBody());
        var friend=request(HttpMethod.POST,"/friends/addFriend/"+guest.id(),host.token(),null);
        status(200,friend);noAccountSecrets(friend.getBody());
        var pending=request(HttpMethod.GET,"/friends/pending",guest.token(),null);
        status(200,pending);noAccountSecrets(pending.getBody());
        noAccountSecrets(request(HttpMethod.GET,"/users/"+host.id()+"/achievements",host.token(),null).getBody());
        noAccountSecrets(request(HttpMethod.PUT,"/users/"+guest.id(),admin.token(),update(guest)).getBody());
    }
    @Test void deleteIsAdminOnlyNoSelfAndNoGameHistoryWithRollbackOnRejection() {
        Account admin=admin(), target=account(), player=account();
        String before=fingerprint(target.id());
        status(401,request(HttpMethod.DELETE,"/users/"+target.id(),null,null));
        status(403,request(HttpMethod.DELETE,"/users/"+target.id(),player.token(),null));
        status(403,request(HttpMethod.DELETE,"/users/"+admin.id(),admin.token(),null));
        assertTrue(before.equals(fingerprint(target.id())));
        var game=request(HttpMethod.POST,"/games",target.token(),Map.of());status(201,game);
        int id=game.getBody().path("id").asInt();
        int cards=count("select count(*) from cards"), achievements=count("select count(*) from achievements where user_id=?",target.id());
        status(403,request(HttpMethod.DELETE,"/users/"+target.id(),admin.token(),null));
        assertTrue(before.equals(fingerprint(target.id())));
        assertEquals(1,count("select count(*) from games where id=?",id));
        assertEquals(1,count("select count(*) from players where game_id=? and user_id=?",id,target.id()));
        assertEquals(cards,count("select count(*) from cards"));
        assertEquals(achievements,count("select count(*) from achievements where user_id=?",target.id()));
    }
    @Test void deleteCannotCascadeFromASpectatorToAnExistingGame() {
        Account admin=admin(), viewer=account(), host=account();
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);
        int id=created.getBody().path("id").asInt();
        jdbc.update("insert into game_spectators(game_id,user_id) values (?,?)",id,viewer.id());
        status(403,request(HttpMethod.DELETE,"/users/"+viewer.id(),admin.token(),null));
        assertEquals(1,count("select count(*) from games where id=?",id));
        assertEquals(1,count("select count(*) from game_spectators where game_id=? and user_id=?",id,viewer.id()));
        assertEquals(1,count("select count(*) from appusers where id=?",viewer.id()));
    }
    @Test void adminCanDeleteAnUnusedAccountWithItsPlaceholderAndPendingSocialData() {
        Account admin=admin(), target=account(), host=account();
        var game=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,game);
        int id=game.getBody().path("id").asInt();
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+target.id(),host.token(),null));
        int friendship=count("select id from friend_requests where sender_id=? and receiver_id=?",host.id(),target.id());
        status(200,request(HttpMethod.PATCH,"/friends/"+friendship+"/accept",target.token(),null));
        status(200,request(HttpMethod.POST,"/games/"+id+"/invite",host.token(),Map.of("invited",String.valueOf(target.id()))));
        status(200,request(HttpMethod.DELETE,"/users/"+target.id(),admin.token(),null));
        assertEquals(0,count("select count(*) from appusers where id=?",target.id()));
        assertEquals(0,count("select count(*) from players where user_id=?",target.id()));
        assertEquals(0,count("select count(*) from game_invitations where invited_id=?",target.id()));
        assertEquals(0,count("select count(*) from friend_requests where sender_id=? or receiver_id=?",target.id(),target.id()));
        assertEquals(1,count("select count(*) from games where id=?",id));
    }
    private Set<String> fields(JsonNode node) { Set<String> names=new HashSet<>();node.fieldNames().forEachRemaining(names::add);return names; }
}
