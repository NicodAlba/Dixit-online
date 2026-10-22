package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

// No test transaction: every HTTP operation commits or rolls back independently.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
class GameLobbyHttpIntegrationTest {
    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;

    record Account(int id, String token) {}

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "secret"})
    void creationPersistsExactlyOneHostParticipation(String password) {
        Account host = account();
        int id = lobby(password, host);
        JsonNode game = read(id, host);
        assertEquals(1, game.path("players").size());
        assertEquals(host.id(), game.path("leader").path("id").asInt());
        assertEquals(1, count("select count(*) from players where game_id = ? and user_id = ?", id, host.id()));
        assertEquals(2, count("select count(*) from players where user_id = ?", host.id()));
        assertTrue(game.path("startDate").isNull());
        assertEquals(0, game.path("turns").size());
        if (password == null || password.isBlank()) assertTrue(game.path("password").isNull());
        else assertEquals(password, game.path("password").asText());
        // No new restriction on owning more than one lobby.
        assertNotEquals(id, lobby(null, host));
    }

    @Test
    void rejectedJoinsLeaveNoResidualParticipation() {
        Account host = account(), guest = account(), third = account(), fourth = account(), outsider = account();
        int id = lobby("secret", host);
        assertStatus(401, request(HttpMethod.PATCH, "/games/" + id + "/join", guest, Map.of("password", "wrong")));
        assertStatus(404, request(HttpMethod.PATCH, "/games/999999999/join", guest, Map.of()));
        assertEquals(1, count("select count(*) from players where user_id = ?", guest.id()));
        join(id, "secret", guest);
        assertStatus(409, request(HttpMethod.PATCH, "/games/" + id + "/join", guest, Map.of("password", "secret")));
        assertEquals(1, count("select count(*) from players where game_id = ? and user_id = ?", id, guest.id()));
        join(id, "secret", third);
        join(id, "secret", fourth);
        ResponseEntity<JsonNode> full = request(HttpMethod.PATCH, "/games/" + id + "/join", outsider, Map.of("password", "secret"));
        assertStatus(409, full);
        assertEquals("Already 4 players in the game", full.getBody().path("message").asText());
        assertEquals(4, read(id, host).path("players").size());
        assertEquals(1, count("select count(*) from players where user_id = ?", outsider.id()));
    }

    @Test
    void concurrentJoinsCannotTakeTheSameLastSlot() throws Exception {
        Account host = account(), guest = account(), third = account(), fourth = account(), fifth = account();
        int id = lobby(null, host, guest, third);
        List<Integer> statuses = race(
            () -> request(HttpMethod.PATCH, "/games/" + id + "/join", fourth, Map.of()).getStatusCode().value(),
            () -> request(HttpMethod.PATCH, "/games/" + id + "/join", fifth, Map.of()).getStatusCode().value());
        assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        assertEquals(4, count("select count(*) from players where game_id = ?", id));
        assertEquals(4, read(id, host).path("players").size());
        assertEquals(3, count("select count(*) from players where user_id in (?, ?)", fourth.id(), fifth.id()));
    }

    @Test
    void invitationIsKeptOnRejectionAndConsumedOnlyAfterSuccessfulJoin() {
        Account host = account(), guest = account(), third = account(), fourth = account(), invitee = account(), outsider = account();
        int id = lobby(null, host, guest, third, fourth);
        int invitation = invite(id, host, invitee);
        assertStatus(403, accept(invitation, outsider));
        assertTrue(hasInvitation(invitation, invitee));
        assertStatus(409, accept(invitation, invitee));
        assertTrue(hasInvitation(invitation, invitee));
        assertEquals(1, count("select count(*) from players where user_id = ?", invitee.id()));
        assertStatus(200, request(HttpMethod.PATCH, "/games/" + id + "/leave", fourth, null));
        assertStatus(200, accept(invitation, invitee));
        assertFalse(hasInvitation(invitation, invitee));
        assertEquals(4, read(id, host).path("players").size());
        assertStatus(404, accept(invitation, invitee));
        assertEquals(1, count("select count(*) from players where game_id = ? and user_id = ?", id, invitee.id()));
    }

    @Test
    void invitationCannotDuplicateAMemberOrJoinAMissingGame() {
        Account host = account(), guest = account(), invitee = account();
        int id = lobby(null, host, guest);
        int duplicate = invite(id, host, guest);
        assertStatus(409, accept(duplicate, guest));
        assertTrue(hasInvitation(duplicate, guest));
        assertEquals(2, read(id, host).path("players").size());
        int missing = invite(id, host, invitee);
        assertStatus(200, request(HttpMethod.PATCH, "/games/" + id + "/leave", host, null));
        assertStatus(404, accept(missing, invitee));
        assertTrue(hasInvitation(missing, invitee));
        assertEquals(1, count("select count(*) from players where user_id = ?", invitee.id()));
    }

    private Account account() {
        String username = "lobby-" + UUID.randomUUID();
        assertStatus(201, request(HttpMethod.POST, "/auth/signup", null,
            Map.of("username", username, "password", "LocalTest123!", "authority", "PLAYER")));
        ResponseEntity<JsonNode> login = request(HttpMethod.POST, "/auth/signin", null,
            Map.of("username", username, "password", "LocalTest123!"));
        assertStatus(200, login);
        return new Account(login.getBody().path("id").asInt(), login.getBody().path("token").asText());
    }

    private int lobby(String password, Account... accounts) {
        Map<String, String> body = new HashMap<>();
        body.put("password", password);
        ResponseEntity<JsonNode> created = request(HttpMethod.POST, "/games", accounts[0], body);
        assertStatus(201, created);
        int id = created.getBody().path("id").asInt();
        for (int i = 1; i < accounts.length; i++) join(id, password, accounts[i]);
        return id;
    }

    private void join(int id, String password, Account account) {
        Map<String, String> body = new HashMap<>();
        body.put("password", password);
        assertStatus(200, request(HttpMethod.PATCH, "/games/" + id + "/join", account, body));
    }

    private int invite(int id, Account host, Account invitee) {
        // Password is optional, including when the lobby has none.
        assertStatus(200, request(HttpMethod.POST, "/games/" + id + "/invite", host,
            Map.of("invited", Integer.toString(invitee.id()))));
        JsonNode invitations = request(HttpMethod.GET, "/games/invitations", invitee, null).getBody();
        for (JsonNode invitation : invitations) {
            if (invitation.path("gameId").asInt() == id) return invitation.path("id").asInt();
        }
        throw new AssertionError("Invitation not persisted");
    }

    private boolean hasInvitation(int id, Account account) {
        for (JsonNode invitation : request(HttpMethod.GET, "/games/invitations", account, null).getBody()) {
            if (invitation.path("id").asInt() == id) return true;
        }
        return false;
    }

    private ResponseEntity<JsonNode> accept(int invitation, Account account) {
        return request(HttpMethod.PATCH, "/games/invitations/" + invitation + "/accept", account, null);
    }

    private JsonNode read(int id, Account account) {
        ResponseEntity<JsonNode> response = request(HttpMethod.GET, "/games/" + id, account, null);
        assertStatus(200, response);
        return response.getBody();
    }

    private ResponseEntity<JsonNode> request(HttpMethod method, String path, Account account, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (account != null) headers.setBearerAuth(account.token());
        return rest.exchange("/api/v1" + path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void assertStatus(int expected, ResponseEntity<JsonNode> response) {
        assertEquals(expected, response.getStatusCode().value(), () -> String.valueOf(response.getBody()));
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (Callable<Integer> task : List.of(first, second)) {
                results.add(pool.submit(() -> { barrier.await(5, TimeUnit.SECONDS); return task.call(); }));
            }
            return List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
        }
    }
}
