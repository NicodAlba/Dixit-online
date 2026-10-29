package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP/security and separate transactions against disposable fixtures only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:security-acceptance-${random.uuid}",
    "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
abstract class SecurityHttpSupport {
    // Explicit opt-in only; CI/local runner supplies a disposable PostgreSQL database.
    @org.springframework.test.context.DynamicPropertySource
    static void productionDatabase(org.springframework.test.context.DynamicPropertyRegistry properties) {
        String url = System.getenv("DIXIT_TEST_POSTGRES_URL");
        if (url == null) return;
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("DIXIT_DB_USERNAME"));
        properties.add("spring.datasource.password", () -> System.getenv("DIXIT_DB_PASSWORD"));
    }
    @Autowired protected TestRestTemplate rest;
    @Autowired protected JdbcTemplate jdbc;
    protected record Account(int id, String name, String password, String token) {}
    protected String unique() { return "security_" + UUID.randomUUID().toString().replace("-", ""); }
    protected ResponseEntity<JsonNode> request(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1" + path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
    protected void status(int expected, ResponseEntity<?> response) {
        assertEquals(expected, response.getStatusCode().value());
    }
    protected Account account() {
        String name = unique(), password = UUID.randomUUID().toString();
        status(201, request(HttpMethod.POST, "/auth/signup", null,
            Map.of("username", name, "password", password, "authority", "PLAYER")));
        return login(name, password);
    }
    protected Account login(String name, String password) {
        var response = request(HttpMethod.POST, "/auth/signin", null, Map.of("username", name, "password", password));
        status(200, response);
        return new Account(response.getBody().path("id").asInt(), name, password, response.getBody().path("token").asText());
    }
    protected Account admin() {
        Account user = account();
        jdbc.update("update appusers set authority_id = ? where id = ?", authority("ADMIN"), user.id());
        return login(user.name(), user.password());
    }
    protected int authority(String name) {
        return jdbc.queryForObject("select id from authorities where authority = ?", Integer.class, name);
    }
    protected int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    protected Map<String,Object> update(Account user) {
        return new HashMap<>(Map.of("username", user.name()));
    }
    protected String fingerprint(int id) {
        return jdbc.queryForMap("select username,password,authority_id from appusers where id = ?", id).toString();
    }
}
