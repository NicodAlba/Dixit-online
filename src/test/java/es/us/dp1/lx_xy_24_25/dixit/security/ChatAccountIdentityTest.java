package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatAccountIdentityTest extends SecurityHttpSupport {
    @Test void authorIdentityComesFromTheAuthenticatedAccountAndSurvivesANameChange() {
        Account host=account(), buddy=account(), outsider=account();
        var created=request(HttpMethod.POST,"/games",host.token(),Map.of());status(201,created);
        int game=created.getBody().path("id").asInt();
        status(200,request(HttpMethod.PATCH,"/games/"+game+"/join",buddy.token(),Map.of()));
        var sent=request(HttpMethod.POST,"/messages/games/"+game,buddy.token(),Map.of("text","A story","player",Map.of("id",host.id())));status(201,sent);
        int player=count("select id from players where user_id=? and game_id=?",buddy.id(),game);
        assertNotEquals(player,buddy.id(),"Fixture must distinguish User.id and Player.id");
        assertEquals(buddy.id(),sent.getBody().path("accountId").asInt());
        assertEquals(buddy.name(),sent.getBody().path("username").asText());
        String renamed=unique();jdbc.update("update appusers set username=? where id=?",renamed,buddy.id());
        var received=request(HttpMethod.GET,"/messages/games/"+game,host.token(),null);status(200,received);
        var message=received.getBody().get(0);Set<String> fields=new HashSet<>();message.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id","accountId","username","text"),fields);
        assertTrue(sent.getBody().path("id").asInt()>0);
        assertEquals(sent.getBody().path("id").asInt(),message.path("id").asInt());
        assertEquals(buddy.id(),message.path("accountId").asInt());assertEquals(renamed,message.path("username").asText());
        status(403,request(HttpMethod.GET,"/messages/games/"+game,outsider.token(),null));
        status(401,request(HttpMethod.GET,"/messages/games/"+game,null,null));
    }
}
