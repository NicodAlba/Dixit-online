package es.us.dp1.lx_xy_24_25.dixit.security;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP transactions: expiry must survive a second request and a fresh login. */
class InvitationLifecycleTest extends SecurityHttpSupport {
    @org.springframework.beans.factory.annotation.Autowired es.us.dp1.lx_xy_24_25.dixit.social.GameInvitationRepository invitations;
    @org.springframework.beans.factory.annotation.Autowired es.us.dp1.lx_xy_24_25.dixit.user.UserRepository users;
    private int lobby(Account host) {
        var response = request(HttpMethod.POST, "/games", host.token(), Map.of());
        status(201, response); return response.getBody().path("id").asInt();
    }
    private int invite(Account recipient, int game) {
        var invitation = new es.us.dp1.lx_xy_24_25.dixit.social.GameInvitation();
        invitation.setInviter("Host"); invitation.setInvited(users.findById(recipient.id()).orElseThrow());
        invitation.setGameId(game); return invitations.save(invitation).getId();
    }
    @Test void listingRetiresMissingAndStartedButPreservesOtherRecipientsAndFullLobbies() {
        Account host=account(), recipient=account(), other=account();
        int open=lobby(host), missing=lobby(host), started=lobby(host);
        int valid=invite(recipient,open), stale=invite(recipient,missing), historical=invite(recipient,started);
        int otherStale=invite(other,missing);
        status(200,request(HttpMethod.PATCH,"/games/"+open+"/join",account().token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+open+"/join",account().token(),Map.of()));
        status(200,request(HttpMethod.PATCH,"/games/"+open+"/join",account().token(),Map.of()));
        jdbc.update("update games set start_date=current_timestamp where id=?",started);
        status(200,request(HttpMethod.PATCH,"/games/"+missing+"/leave",host.token(),null));
        var list=request(HttpMethod.GET,"/games/invitations",recipient.token(),null); status(200,list);
        assertEquals(1,list.getBody().size()); assertEquals(valid,list.getBody().get(0).path("id").asInt());
        assertEquals(0,count("select count(*) from game_invitations where id in (?,?)",stale,historical));
        assertEquals(1,count("select count(*) from game_invitations where id=?",otherStale));
        status(409,request(HttpMethod.PATCH,"/games/invitations/"+valid+"/accept",recipient.token(),null));
        assertEquals(1,count("select count(*) from game_invitations where id=?",valid));
        var fresh=login(recipient.name(),recipient.password());
        assertEquals(1,request(HttpMethod.GET,"/games/invitations",fresh.token(),null).getBody().size());
    }
    @Test void deletedBetweenListAndAcceptanceReturnsGoneAndCommitsExpiry() {
        Account host=account(), recipient=account(), stranger=account(); int game=lobby(host), id=invite(recipient,game);
        status(200,request(HttpMethod.GET,"/games/invitations",recipient.token(),null));
        status(200,request(HttpMethod.PATCH,"/games/"+game+"/leave",host.token(),null));
        status(403,request(HttpMethod.PATCH,"/games/invitations/"+id+"/accept",stranger.token(),null));
        assertEquals(1,count("select count(*) from game_invitations where id=?",id));
        var expired=request(HttpMethod.PATCH,"/games/invitations/"+id+"/accept",recipient.token(),null);
        status(410,expired); assertEquals("This invitation is no longer available.",expired.getBody().path("message").asText());
        assertEquals(0,count("select count(*) from game_invitations where id=?",id));
        assertTrue(request(HttpMethod.GET,"/games/invitations",login(recipient.name(),recipient.password()).token(),null).getBody().isEmpty());
        status(410,request(HttpMethod.PATCH,"/games/invitations/"+id+"/accept",recipient.token(),null));
    }
    @Test void validAcceptsOnceAndStartedHistoryExpiresWithoutDeletingGame() {
        Account host=account(), recipient=account(); int game=lobby(host), id=invite(recipient,game);
        status(200,request(HttpMethod.PATCH,"/games/invitations/"+id+"/accept",recipient.token(),null));
        assertEquals(1,count("select count(*) from players where game_id=? and user_id=?",game,recipient.id()));
        assertEquals(0,count("select count(*) from game_invitations where id=?",id));
        Account late=account(); int expired=invite(late,game);
        jdbc.update("update games set start_date=current_timestamp,end_date=current_timestamp,cancelled=true where id=?",game);
        status(410,request(HttpMethod.PATCH,"/games/invitations/"+expired+"/accept",late.token(),null));
        assertEquals(0,count("select count(*) from game_invitations where id=?",expired));
        assertEquals(1,count("select count(*) from games where id=?",game));
    }
}
