package es.us.dp1.lx_xy_24_25.dixit.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.ContainerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Explicit, local evidence collector: mvn -Dtest=SecurityPrivacyProbe test.
 * The name deliberately excludes it from the default *Test suite. Successful execution
 * means observations were collected, NOT that the application passed security acceptance.
 * No production/external target is accepted: TestRestTemplate uses this ephemeral random-port server.
 * All credentials are random, kept in memory, and excluded from reports and assertion messages.
 */
@Tag("security-spec-evidence")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:security-spec-${random.uuid}",
    "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
class SecurityPrivacyProbe {
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @LocalServerPort int port;
    private final Map<String, Object> observations = new LinkedHashMap<>();
    private record Account(int id, String name, String credential, String token) {}
    private record Match(int id, List<Account> players) {}

    @Test
    void accountsAndAuthority() throws Exception {
        Account player = account();
        int before = count("select count(*) from appusers");
        var adminSignup = req(HttpMethod.POST, "/auth/signup", null,
            Map.of("username", name(), "password", UUID.randomUUID().toString(), "authority", "ADMIN"));
        observation("public_signup_admin", adminSignup, Map.of("createdUsers", count("select count(*) from appusers") - before));
        String elevatedName = name(), credential = UUID.randomUUID().toString();
        var signup = req(HttpMethod.POST, "/auth/signup", null, Map.of("username", elevatedName, "password", credential, "authority", "AdMiN"));
        var login = req(HttpMethod.POST, "/auth/signin", null, Map.of("username", elevatedName, "password", credential));
        String token = login.getBody().path("token").asText();
        observation("mixed_case_admin_can_use_admin_resource", req(HttpMethod.GET, "/games", token, null),
            Map.of("signupStatus", signup.getStatusCode().value(), "adminInDatabase", role(elevatedName).equals("ADMIN")));
        int beforeMissingAuthority = count("select count(*) from appusers");
        var missing = req(HttpMethod.POST, "/auth/signup", null, Map.of("username", name(), "password", UUID.randomUUID().toString()));
        observation("signup_missing_authority", missing, Map.of("databaseUnchanged", beforeMissingAuthority == count("select count(*) from appusers")));
        int adminId = authority("ADMIN"), playerId = authority("PLAYER");
        var rawCreate = req(HttpMethod.POST, "/users", null,
            Map.of("username", name(), "password", UUID.randomUUID().toString(), "authority", Map.of("id", adminId)));
        observation("public_users_create_admin", rawCreate, Map.of("adminInDatabase", role(rawCreate.getBody().path("username").asText()).equals("ADMIN"), "readContainsPasswordHash", hasHash(rawCreate.getBody())));
        var publicList = req(HttpMethod.GET, "/users", null, null);
        observation("public_user_list", publicList, Map.of("containsHashes", hasHash(publicList.getBody())));
        var publicRead = req(HttpMethod.GET, "/users/" + player.id(), null, null);
        observation("public_user_read_hash", publicRead, Map.of("containsHash", hasHash(publicRead.getBody())));
        observation("player_admin_resource_denied", req(HttpMethod.GET, "/games", player.token(), null), Map.of());

        Account promoted = account();
        var changeRole = req(HttpMethod.PUT, "/users/" + promoted.id(), null,
            Map.of("username", promoted.name(), "authority", Map.of("id", adminId)));
        var afterLogin = req(HttpMethod.POST, "/auth/signin", null, Map.of("username", promoted.name(), "password", promoted.credential()));
        observation("anonymous_changes_other_authority", changeRole, Map.of("adminInDatabase", role(promoted.name()).equals("ADMIN"),
            "adminResourceStatus", req(HttpMethod.GET, "/games", afterLogin.getBody().path("token").asText(), null).getStatusCode().value()));

        Account target = account();
        String changedCredential = UUID.randomUUID().toString();
        var passwordChange = req(HttpMethod.PUT, "/users/" + target.id(), player.token(),
            Map.of("username", target.name(), "password", changedCredential, "authority", Map.of("id", playerId)));
        int elevatedId = login.getBody().path("id").asInt();
        String adminCredential = UUID.randomUUID().toString();
        var resetAdmin = req(HttpMethod.PUT, "/users/" + elevatedId, null,
            Map.of("username", elevatedName, "password", adminCredential, "authority", Map.of("id", adminId)));
        var stolenLogin = req(HttpMethod.POST, "/auth/signin", null, Map.of("username", elevatedName, "password", adminCredential));
        observation("anonymous_resets_test_admin_credential", resetAdmin, Map.of("adminResourceStatus",
            req(HttpMethod.GET, "/games", stolenLogin.getBody().path("token").asText(), null).getStatusCode().value()));
        String stored = jdbc.queryForObject("select password from appusers where id = ?", String.class, target.id());
        observation("player_changes_other_password", passwordChange, Map.of("newCredentialPersisted", encoder.matches(changedCredential, stored),
            "oldLoginStatus", req(HttpMethod.POST, "/auth/signin", null, Map.of("username", target.name(), "password", target.credential())).getStatusCode().value()));

        Account deletion = account();
        var anonymousDelete = req(HttpMethod.DELETE, "/users/" + deletion.id(), null, null);
        observation("anonymous_delete_user", anonymousDelete, Map.of("rowUnchanged", count("select count(*) from appusers where id = ?", deletion.id()) == 1));
        var otherDelete = req(HttpMethod.DELETE, "/users/" + deletion.id(), player.token(), null);
        observation("player_deletes_other_user", otherDelete, Map.of("rowDeleted", count("select count(*) from appusers where id = ?", deletion.id()) == 0));
        save("accounts");
    }

    @Test
    void membershipVisibilityAndStomp() throws Exception {
        List<Account> players = List.of(account(), account(), account());
        Account outsider = account(), spectator = account();
        for (Account player : players) {
            assertStatus(200, req(HttpMethod.POST, "/friends/addFriend/" + spectator.id(), player.token(), null));
            int friendship = jdbc.queryForObject("select id from friend_requests where sender_id = ? and receiver_id = ?", Integer.class, player.id(), spectator.id());
            assertStatus(200, req(HttpMethod.PATCH, "/friends/" + friendship + "/accept", spectator.token(), null));
        }
        Match match = start(players);
        int id = match.id();
        int tid = count("select max(id) from turns where game_id = ?", id);
        var game = req(HttpMethod.GET, "/games/" + id, players.getFirst().token(), null);
        observation("member_game_privacy", game, Map.of("containsHashes", hasHash(game.getBody()), "lobbyPasswordPresent", !game.getBody().path("password").isNull(), "visibleHands", visibleHands(game.getBody())));
        observation("anonymous_game_read", req(HttpMethod.GET, "/games/" + id, null, null), Map.of());
        observation("outsider_game_read", req(HttpMethod.GET, "/games/" + id, outsider.token(), null), Map.of());
        var allGames = req(HttpMethod.GET, "/games/inProgress", outsider.token(), null);
        observation("outsider_games_in_progress", allGames, Map.of("containsHashes", hasHash(allGames.getBody()), "gamesVisible", allGames.getBody().size()));
        observation("outsider_turn_read", req(HttpMethod.GET, "/turns/" + tid, outsider.token(), null), Map.of());
        int foreignPlayer = playerId(id, players.getFirst());
        var playerRead = req(HttpMethod.GET, "/players/" + foreignPlayer, outsider.token(), null);
        observation("outsider_player_read", playerRead, Map.of("visibleCards", playerRead.getBody().path("cards").size(), "containsHashes", hasHash(playerRead.getBody())));
        observation("own_cards_route", req(HttpMethod.GET, "/players/" + foreignPlayer + "/cards", players.getFirst().token(), null), Map.of());
        var view = req(HttpMethod.GET, "/games/" + id, spectator.token(), null);
        observation("authorized_spectator_game", view, Map.of("visibleHands", visibleHands(view.getBody()), "containsHashes", hasHash(view.getBody())));
        observation("spectator_unchecked_perspective_id", req(HttpMethod.PATCH, "/games/" + id + "/spectate/999999999", spectator.token(), null), Map.of());
        observation("outsider_spectate_denied", req(HttpMethod.PATCH, "/games/" + id + "/spectate/" + foreignPlayer, outsider.token(), null), Map.of());
        assertStatus(201, req(HttpMethod.POST, "/messages/games/" + id, players.getFirst().token(), Map.of("text", "spec fixture")));
        observation("outsider_chat_read", req(HttpMethod.GET, "/messages/games/" + id, outsider.token(), null), Map.of());
        int messageCount = count("select count(*) from messages where game_id = ?", id);
        observation("outsider_chat_write_denied", req(HttpMethod.POST, "/messages/games/" + id, outsider.token(), Map.of("text", "spec fixture")),
            Map.of("rowsUnchanged", messageCount == count("select count(*) from messages where game_id = ?", id)));
        var invitation = req(HttpMethod.POST, "/games/" + id + "/invite", outsider.token(), Map.of("invited", String.valueOf(spectator.id())));
        observation("outsider_sends_invitation_after_start", invitation, Map.of());
        int invitationId = req(HttpMethod.GET, "/games/invitations", spectator.token(), null).getBody().get(0).path("id").asInt();
        int invitationCount = count("select count(*) from game_invitations where id = ?", invitationId);
        observation("wrong_invitation_recipient_denied", req(HttpMethod.PATCH, "/games/invitations/" + invitationId + "/accept", players.getFirst().token(), null),
            Map.of("rowUnchanged", invitationCount == count("select count(*) from game_invitations where id = ?", invitationId)));
        int storyteller = game.getBody().path("turns").get(0).path("storyteller").path("id").asInt();
        int answer = firstCard(storyteller);
        var container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(512 * 1024);
        WebSocketStompClient stomp = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient(container)))));
        stomp.setInboundMessageSizeLimit(512 * 1024); stomp.setMessageConverter(new MappingJackson2MessageConverter()); stomp.start();
        StompSession session = null;
        try {
            StompSession noCredentials = stomp.connectAsync("http://127.0.0.1:" + port + "/ws", new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            observations.put("stomp_no_credentials_connect", Map.of("connected", noCredentials.isConnected()));
            noCredentials.disconnect();
            StompHeaders headers = new StompHeaders(); headers.add("Authorization", "Bearer invalid-probe");
            session = stomp.connectAsync("http://127.0.0.1:" + port + "/ws", new org.springframework.web.socket.WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            observations.put("stomp_invalid_token_connect", Map.of("connected", session.isConnected()));
            BlockingQueue<JsonNode> games = new LinkedBlockingQueue<>(), turns = new LinkedBlockingQueue<>();
            String gameDestination = "/topic/game/" + id, turnDestination = "/topic/turn/" + tid;
            session.subscribe(gameDestination, frames(games)); session.subscribe(turnDestination, frames(turns));
            session.send(gameDestination, Map.of("id", id, "players", List.of(), "turns", List.of(), "probe", true));
            JsonNode forged = games.poll(5, TimeUnit.SECONDS);
            observations.put("stomp_client_can_publish_server_shaped_state", Map.of("received", forged != null && forged.path("probe").asBoolean()));
            var selected = req(HttpMethod.PUT, "/turns/" + tid + "/answer/" + answer, outsider.token(), null);
            observation("outsider_selects_storyteller_card", selected, Map.of("answerPersisted", count("select count(*) from turns where id = ? and answer_id = ?", tid, answer) == 1));
            JsonNode event = games.poll(5, TimeUnit.SECONDS), turn = turns.poll(5, TimeUnit.SECONDS);
            observations.put("stomp_unauthorized_game_event", Map.of("received", event != null, "visibleHands", event == null ? 0 : visibleHands(event), "containsHashes", event != null && hasHash(event), "lobbyPasswordPresent", event != null && !event.path("password").isNull()));
            observations.put("stomp_unauthorized_turn_event", Map.of("received", turn != null, "answerVisible", turn != null && !turn.path("answer").isNull()));
        } finally {
            if (session != null && session.isConnected()) session.disconnect(); stomp.stop();
        }
        var clue = req(HttpMethod.PUT, "/turns/" + tid + "/clue", outsider.token(), "spec clue");
        observation("outsider_sends_clue", clue, Map.of());
        int victimPlayer = 0;
        for (Account player : players) {
            int pid = playerId(id, player);
            if (pid != storyteller) {
                assertStatus(200, req(HttpMethod.POST, "/turns/" + tid + "/play-card/" + firstCard(pid) + "/" + pid, player.token(), null));
                victimPlayer = pid;
            }
        }
        observation("outsider_votes_as_other_player", req(HttpMethod.POST, "/turns/" + tid + "/vote/" + victimPlayer + "/" + answer, outsider.token(), null),
            Map.of("foreignVotePersisted", count("select count(*) from player_votes where card_id = ? and player_id = ?", answer, victimPlayer) == 1));
        var table = req(HttpMethod.GET, "/turns/" + tid, outsider.token(), null).getBody();
        observations.put("table_during_voting", Map.of("answerVisible", !table.path("answer").isNull(), "ownersVisible", table.path("playedCards").get(0).hasNonNull("owner"), "voterIdentitiesVisible", table.path("playedCards").toString().contains("\"votes\":[{")));
        save("game-and-stomp");
    }

    @Test
    void phasesAndVoting() throws Exception {
        List<Account> players = List.of(account(), account(), account()); Account outsider = account();
        Match earlyClue = start(players);
        int earlyTurn = count("select max(id) from turns where game_id = ?", earlyClue.id());
        observation("clue_before_answer_from_outsider", req(HttpMethod.PUT, "/turns/" + earlyTurn + "/clue", outsider.token(), "spec clue"),
            Map.of("phaseAdvancedWithoutAnswer", count("select count(*) from turns where id = ? and answer_id is null and state = 'WAITING_PLAYER_CARDS'", earlyTurn) == 1));
        Match match = start(players); int id = match.id();
        int tid = count("select max(id) from turns where game_id = ?", id);
        int storyteller = count("select storyteller_id from turns where id = ?", tid);
        List<Account> voters = players.stream().filter(p -> playerId(id, p) != storyteller).toList();
        int victim = playerId(id, voters.getFirst()), victimCard = firstCard(victim);
        observation("outsider_plays_as_other_before_clue", req(HttpMethod.POST, "/turns/" + tid + "/play-card/" + victimCard + "/" + victim, outsider.token(), null),
            Map.of("cardPersisted", count("select count(*) from cards where id = ? and turn_id = ?", victimCard, tid) == 1));
        Account narrator = players.stream().filter(p -> playerId(id, p) == storyteller).findFirst().orElseThrow();
        int answer = firstCard(storyteller);
        assertStatus(200, req(HttpMethod.PUT, "/turns/" + tid + "/answer/" + answer, narrator.token(), null));
        assertStatus(200, req(HttpMethod.PUT, "/turns/" + tid + "/clue", narrator.token(), "spec clue"));
        int secondPlayer = playerId(id, voters.getLast());
        assertStatus(200, req(HttpMethod.POST, "/turns/" + tid + "/play-card/" + firstCard(secondPlayer) + "/" + secondPlayer, voters.getLast().token(), null));
        observation("self_vote", req(HttpMethod.POST, "/turns/" + tid + "/vote/" + victim + "/" + victimCard, voters.getFirst().token(), null),
            Map.of("votePersisted", count("select count(*) from player_votes where card_id = ? and player_id = ?", victimCard, victim) == 1));
        observation("storyteller_vote", req(HttpMethod.POST, "/turns/" + tid + "/vote/" + storyteller + "/" + answer, narrator.token(), null),
            Map.of("advancedWithoutOtherVoter", count("select count(*) from turns where game_id = ?", id) == 2));
        List<Map<String, Object>> before = state(id);
        var duplicate = req(HttpMethod.PATCH, "/games/" + id + "/start", players.getFirst().token(), null);
        observation("repeated_start_denied", duplicate, Map.of("databaseUnchanged", before.equals(state(id))));
        save("phases");
    }

    private Account account() {
        String name = name(), credential = UUID.randomUUID().toString();
        assertStatus(201, req(HttpMethod.POST, "/auth/signup", null, Map.of("username", name, "password", credential, "authority", "PLAYER")));
        var login = req(HttpMethod.POST, "/auth/signin", null, Map.of("username", name, "password", credential)); assertStatus(200, login);
        return new Account(login.getBody().path("id").asInt(), name, credential, login.getBody().path("token").asText());
    }
    private Match start(List<Account> players) {
        String credential = UUID.randomUUID().toString();
        var created = req(HttpMethod.POST, "/games", players.getFirst().token(), Map.of("password", credential)); assertStatus(201, created);
        int id = created.getBody().path("id").asInt();
        for (Account player : players.subList(1, players.size())) assertStatus(200, req(HttpMethod.PATCH, "/games/" + id + "/join", player.token(), Map.of("password", credential)));
        List<Map<String, Object>> before = state(id);
        var denied = req(HttpMethod.PATCH, "/games/" + id + "/start", players.get(1).token(), null);
        observations.put("nonhost_start_denied", Map.of("status", denied.getStatusCode().value(), "databaseUnchanged", before.equals(state(id))));
        assertStatus(200, req(HttpMethod.PATCH, "/games/" + id + "/start", players.getFirst().token(), null));
        return new Match(id, players);
    }
    private List<Map<String, Object>> state(int id) {
        List<Map<String, Object>> data = new ArrayList<>(jdbc.queryForList("select id,start_date,end_date from games where id = ?", id));
        data.addAll(jdbc.queryForList("select id, points from players where game_id = ? order by id", id));
        data.addAll(jdbc.queryForList("select id, state from turns where game_id = ? order by id", id));
        data.addAll(jdbc.queryForList("select id, player_id, turn_id from cards where game_id = ? order by id", id));
        return data;
    }
    private int playerId(int game, Account account) { return count("select id from players where game_id = ? and user_id = ?", game, account.id()); }
    private int firstCard(int player) { return count("select min(id) from cards where player_id = ? and turn_id is null", player); }
    private int authority(String name) { return count("select id from authorities where authority = ?", name); }
    private String role(String name) { return jdbc.queryForObject("select a.authority from appusers u join authorities a on a.id = u.authority_id where u.username = ?", String.class, name); }
    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private String name() { return "spec-" + UUID.randomUUID(); }
    private void assertStatus(int expected, ResponseEntity<JsonNode> response) { assertEquals(expected, response.getStatusCode().value()); }
    private int visibleHands(JsonNode game) { int result = 0; for (JsonNode p : game.path("players")) if (!p.path("cards").isEmpty()) result++; return result; }
    private boolean hasHash(JsonNode node) {
        if (node == null) return false;
        if (node.isObject() && node.has("password") && node.path("password").asText().startsWith("$2")) return true;
        for (JsonNode child : node) if (hasHash(child)) return true;
        return false;
    }
    private ResponseEntity<JsonNode> req(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return rest.exchange("/api/v1" + path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
    private void observation(String id, ResponseEntity<JsonNode> response, Map<String, Object> detail) {
        Map<String, Object> row = new LinkedHashMap<>(detail); row.put("status", response.getStatusCode().value()); observations.put(id, row);
    }
    private StompFrameHandler frames(BlockingQueue<JsonNode> messages) {
        return new StompFrameHandler() {
            public Type getPayloadType(StompHeaders headers) { return JsonNode.class; }
            public void handleFrame(StompHeaders headers, Object value) { messages.add((JsonNode) value); }
        };
    }
    private void save(String section) throws Exception {
        Map<String, Object> report = Map.of("purpose", "observations_only_not_security_acceptance", "section", section, "observations", observations);
        Files.createDirectories(Path.of("target"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("target/security-spec-" + section + ".json").toFile(), report);
        System.out.println("SECURITY_SPEC_EVIDENCE " + mapper.writeValueAsString(report));
    }
}
