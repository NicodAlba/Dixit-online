package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;

class DevelopmentConsoleTest extends SecurityHttpSupport {
    @Autowired ApplicationContext context;
    @Test void anonymousConsoleIsUnavailableByDefaultWhilePublicAndAuthenticatedApisStillWork(){
        var response=rest.getForEntity("/h2-console/",String.class);
        assertEquals(401,response.getStatusCode().value(),"The default application must not expose the database console");
        assertTrue(context.getBeansOfType(ServletRegistrationBean.class).values().stream().noneMatch(b->b.getUrlMappings().contains("/h2-console/*")));
        status(200,request(HttpMethod.GET,"/developers",null,null));
        status(401,request(HttpMethod.GET,"/games/my",null,null));
        status(200,request(HttpMethod.GET,"/games/my",account().token(),null));
    }
}
