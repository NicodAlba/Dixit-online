package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountWritePermissionsTest extends SecurityHttpSupport {
    @ParameterizedTest
    @ValueSource(strings={"ADMIN","AdMiN","UNKNOWN",""})
    void rejectsPublicPrivilegesWithoutPartialParticipation(String role) {
        int users=count("select count(*) from appusers"), players=count("select count(*) from players");
        status(400,request(HttpMethod.POST,"/auth/signup",null,
            Map.of("username",unique(),"password",UUID.randomUUID().toString(),"authority",role)));
        assertEquals(users,count("select count(*) from appusers"));
        assertEquals(players,count("select count(*) from players"));
    }
    @Test void acceptsPlayerCanonicalizesAndRequiresAuthority() {
        String name=unique(), password=UUID.randomUUID().toString();
        status(201,request(HttpMethod.POST,"/auth/signup",null,Map.of("username",name,"password",password,"authority","pLaYeR")));
        Account player=login(name,password);
        assertEquals(authority("PLAYER"),count("select authority_id from appusers where id=?",player.id()));
        assertEquals(1,count("select count(*) from players where user_id=? and game_id is null",player.id()));
        status(400,request(HttpMethod.POST,"/auth/signup",null,Map.of("username",unique(),"password",password)));
    }
    @Test void createRequiresAdminAndCannotOverwriteOrBindRelations() {
        Account admin=admin(), player=account();
        var body=new HashMap<String,Object>(Map.of("username",unique(),"password",UUID.randomUUID().toString(),"authority",Map.of("id",authority("ADMIN"))));
        int before=count("select count(*) from appusers");
        status(401,request(HttpMethod.POST,"/users",null,body));
        status(403,request(HttpMethod.POST,"/users",player.token(),body));
        assertEquals(before,count("select count(*) from appusers"));
        body.put("players",List.of(Map.of("id",count("select min(id) from players where user_id=?",player.id()))));
        status(201,request(HttpMethod.POST,"/users",admin.token(),body));
        int created=count("select id from appusers where username=?",body.get("username"));
        assertEquals(authority("ADMIN"),count("select authority_id from appusers where id=?",created));
        assertEquals(1,count("select count(*) from players where user_id=?",player.id()));
        body.put("id",player.id());
        String beforeUser=fingerprint(player.id());
        status(400,request(HttpMethod.POST,"/users",admin.token(),body));
        assertTrue(beforeUser.equals(fingerprint(player.id())),"Rejected overwrite must not mutate account");
    }
    @Test void rejectsAnonymousOtherAccountAndSpoofedAuthority() {
        Account owner=account(), other=account(), admin=admin();
        var body=update(owner); body.put("password",UUID.randomUUID().toString());
        String before=fingerprint(owner.id()), adminBefore=fingerprint(admin.id());
        status(401,request(HttpMethod.PUT,"/users/"+owner.id(),null,body));
        status(403,request(HttpMethod.PUT,"/users/"+owner.id(),other.token(),body));
        status(403,request(HttpMethod.PUT,"/users/"+admin.id(),other.token(),body));
        body.put("authority",Map.of("id",authority("ADMIN"),"authority","PLAYER"));
        status(403,request(HttpMethod.PUT,"/users/"+owner.id(),owner.token(),body));
        assertTrue(before.equals(fingerprint(owner.id())),"Rejected edits must not mutate credentials or role");
        assertTrue(adminBefore.equals(fingerprint(admin.id())),"Admin credentials must remain intact");
        login(owner.name(),owner.password());login(admin.name(),admin.password());
    }
    @Test void ownerCanChangePasswordAndOmittedOrSameRolePreservesAuthority() {
        Account owner=account(); String password=UUID.randomUUID().toString();
        var body=update(owner); body.put("password",password);
        status(200,request(HttpMethod.PUT,"/users/"+owner.id(),owner.token(),body));
        Account current=login(owner.name(),password);
        status(401,request(HttpMethod.POST,"/auth/signin",null,Map.of("username",owner.name(),"password",owner.password())));
        body=update(current);body.put("authority",Map.of("id",authority("PLAYER")));
        status(200,request(HttpMethod.PUT,"/users/"+owner.id(),current.token(),body));
        assertEquals(authority("PLAYER"),count("select authority_id from appusers where id=?",owner.id()));
        login(owner.name(),password);
    }
    @Test void adminCanEditNameAndRoleButCannotResetOthersPassword() {
        Account admin=admin(), target=account();
        var body=update(target);body.put("username",unique());body.put("authority",Map.of("id",authority("ADMIN")));
        status(200,request(HttpMethod.PUT,"/users/"+target.id(),admin.token(),body));
        login((String)body.get("username"),target.password());
        assertEquals(authority("ADMIN"),count("select authority_id from appusers where id=?",target.id()));
        String before=fingerprint(target.id());body.put("password",UUID.randomUUID().toString());
        status(403,request(HttpMethod.PUT,"/users/"+target.id(),admin.token(),body));
        assertTrue(before.equals(fingerprint(target.id())),"Administrative password reset must not mutate anything");
        body.remove("password");body.put("authority",Map.of("id",999999));
        status(400,request(HttpMethod.PUT,"/users/"+target.id(),admin.token(),body));
        assertTrue(before.equals(fingerprint(target.id())));
    }
}
