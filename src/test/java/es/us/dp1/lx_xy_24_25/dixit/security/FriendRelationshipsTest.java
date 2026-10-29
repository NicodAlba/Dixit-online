package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class FriendRelationshipsTest extends SecurityHttpSupport {
    @Test void relationshipsOnlyDescribeTheActorsOwnPublicAccountLinks() {
        Account actor=account(), outgoing=account(), incoming=account(), unrelated=account();
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+outgoing.id(),actor.token(),null));
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+actor.id(),incoming.token(),null));
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+unrelated.id(),outgoing.token(),null));
        var result=request(HttpMethod.GET,"/friends/relationships",actor.token(),null);status(200,result);
        assertEquals(2,result.getBody().size());
        result.getBody().forEach(link->{
            Set<String> fields=new HashSet<>();link.fieldNames().forEachRemaining(fields::add);
            assertEquals(Set.of("accountId","username","status","requestId"),fields);
            assertNotEquals(unrelated.id(),link.path("accountId").asInt());
            assertEquals(link.path("accountId").asInt()==outgoing.id()?"OUTGOING":"INCOMING",link.path("status").asText());
        });
        int requestId=count("select id from friend_requests where sender_id=? and receiver_id=?",actor.id(),outgoing.id());
        status(200,request(HttpMethod.PATCH,"/friends/"+requestId+"/accept",outgoing.token(),null));
        assertTrue(request(HttpMethod.GET,"/friends/relationships",actor.token(),null).getBody().toString().contains("FRIEND"));
        status(401,request(HttpMethod.GET,"/friends/relationships",null,null));
        status(403,request(HttpMethod.GET,"/friends/relationships",admin().token(),null));
    }
    @Test void repeatedCrossedAndSelfRequestsAreRejectedWithoutDuplicates() {
        Account first=account(), second=account();
        status(400,request(HttpMethod.POST,"/friends/addFriend/"+first.id(),first.token(),null));
        status(200,request(HttpMethod.POST,"/friends/addFriend/"+second.id(),first.token(),null));
        status(409,request(HttpMethod.POST,"/friends/addFriend/"+second.id(),first.token(),null));
        status(409,request(HttpMethod.POST,"/friends/addFriend/"+first.id(),second.token(),null));
        assertEquals(1,count("select count(*) from friend_requests where (sender_id=? and receiver_id=?) or (sender_id=? and receiver_id=?)",first.id(),second.id(),second.id(),first.id()));
    }
    @Test void simultaneousCrossedRequestsCreateOneRelationship() throws Exception {
        Account first=account(), second=account();
        var executor=Executors.newFixedThreadPool(2);
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Integer> forward=()->{ready.countDown();start.await();return request(HttpMethod.POST,"/friends/addFriend/"+second.id(),first.token(),null).getStatusCode().value();};
            Callable<Integer> reverse=()->{ready.countDown();start.await();return request(HttpMethod.POST,"/friends/addFriend/"+first.id(),second.token(),null).getStatusCode().value();};
            Future<Integer> one=executor.submit(forward), two=executor.submit(reverse);
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            assertEquals(Set.of(200,409),Set.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS)));
            assertEquals(1,count("select count(*) from friend_requests where (sender_id=? and receiver_id=?) or (sender_id=? and receiver_id=?)",first.id(),second.id(),second.id(),first.id()));
        } finally {start.countDown();executor.shutdownNow();}
    }
}
