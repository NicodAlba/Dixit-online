package es.us.dp1.lx_xy_24_25.dixit.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties="DIXIT_H2_CONSOLE_ENABLED=true")
class DevelopmentConsoleOptInTest extends SecurityHttpSupport {
    @Test void explicitLocalOptInRetainsTheDebugConsole(){
        var response=rest.getForEntity("/h2-console/",String.class);
        assertEquals(200,response.getStatusCode().value());
        assertNotNull(response.getBody());assertTrue(response.getBody().contains("login.jsp"));
    }
}
