package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FriendSummaryPrivacyTest extends SecurityHttpSupport {
    void friends(Account a, Account b) {
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+b.id(),a.token(),null));
        int id=count("select id from friend_requests where sender_id=? and receiver_id=?",a.id(),b.id());
        status(200,request(HttpMethod.PATCH,"/friends/"+id+"/accept",b.token(),null));
    }
    @Test void listsNamesWithoutGraphsAndOnlyAuthorizedSpectatorDestination() {
        Account host=account(), second=account(), third=account(), viewer=account(), partialFriend=account();
        for(Account p:List.of(host,second,third)) friends(p,viewer);
        friends(host,partialFriend);
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of("password","private-fixture"));status(201,created);
        int game=created.getBody().path("id").asInt();
        for(Account p:List.of(second,third)) status(200,request(HttpMethod.PATCH,"/games/"+game+"/join",p.token(),Map.of("password","private-fixture")));
        var lobby=request(HttpMethod.GET,"/friends",viewer.token(),null);status(200,lobby);
        lobby.getBody().forEach(f->assertFalse(f.has("spectatableGame")));
        status(200,request(HttpMethod.PATCH,"/games/"+game+"/start",host.token(),null));
        var allowed=request(HttpMethod.GET,"/friends",viewer.token(),null);status(200,allowed);
        assertEquals(3,allowed.getBody().size());
        allowed.getBody().forEach(f->{
            assertTrue(f.hasNonNull("username"));
            assertFalse(f.has("password"));assertFalse(f.has("players"));assertFalse(f.has("authority"));assertFalse(f.has("gamesToSpectate"));
            assertEquals(game,f.path("spectatableGame").path("id").asInt());
            int player=f.path("spectatableGame").path("playerId").asInt();
            assertEquals(1,count("select count(*) from players where id=? and game_id=? and user_id=?",player,game,f.path("id").asInt()));
            status(200,request(HttpMethod.PATCH,"/games/"+game+"/spectate/"+player,viewer.token(),null));
        });
        var denied=request(HttpMethod.GET,"/friends",partialFriend.token(),null);status(200,denied);
        assertEquals(1,denied.getBody().size());assertFalse(denied.getBody().get(0).has("spectatableGame"));
        jdbc.update("update games set end_date=current_timestamp where id=?",game);
        request(HttpMethod.GET,"/friends",viewer.token(),null).getBody().forEach(f->assertFalse(f.has("spectatableGame")));
    }
}
