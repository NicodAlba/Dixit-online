package es.us.dp1.lx_xy_24_25.dixit.game;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;
import es.us.dp1.lx_xy_24_25.dixit.card.CardService;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

// No test transaction: every HTTP operation commits or rolls back independently.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:lobby-http-${random.uuid}",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
class GameLobbyHttpIntegrationTest {
    @Autowired private TestRestTemplate rest;
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @SpyBean private CardService cardService;
    @SpyBean private GameRepository gameRepository;
    @SpyBean private SimpMessagingTemplate messagingTemplate;

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

    @Test
    void onlyHostCanStartWithThreeOrFourPlayersAndCannotStartTwice() {
        Account host = account(), second = account(), third = account(), fourth = account(), outsider = account();
        int id = lobby(null, host);
        assertStatus(400, start(id, host));
        assertUnstarted(id, host);
        join(id, null, second);
        assertStatus(400, start(id, host));
        assertUnstarted(id, host);
        join(id, null, third);
        assertStatus(403, start(id, second));
        assertStatus(403, start(id, outsider));
        assertUnstarted(id, host);
        assertStatus(200, start(id, host));
        JsonNode started = assertStarted(id, host, 3);
        assertStatus(400, start(id, host));
        assertEquals(started.path("startDate"), read(id, host).path("startDate"));
        assertStarted(id, host, 3);
        int four = lobby(null, host, second, third, fourth);
        assertStatus(200, start(four, host));
        assertStarted(four, host, 4);
    }

    @Test
    void startedAndFinishedGamesRejectBothEntryPathsAndRestart() {
        Account host = account(), second = account(), third = account(), outsider = account();
        int id = lobby(null, host, second, third);
        int invitation = invite(id, host, outsider);
        assertStatus(200, start(id, host));
        assertStatus(409, request(HttpMethod.PATCH, "/games/" + id + "/join", outsider, Map.of()));
        assertStatus(409, accept(invitation, outsider));
        assertTrue(hasInvitation(invitation, outsider));
        finishGame(id, host, second, third);
        JsonNode finished = read(id, host);
        assertFalse(finished.path("endDate").isNull());
        assertEquals(6, finished.path("turns").size());
        assertStatus(409, request(HttpMethod.PATCH, "/games/" + id + "/join", outsider, Map.of()));
        assertStatus(409, accept(invitation, outsider));
        assertTrue(hasInvitation(invitation, outsider));
        assertStatus(400, start(id, host));
        assertEquals(finished.path("endDate"), read(id, host).path("endDate"));
        assertEquals(6, count("select count(*) from turns where game_id = ?", id));
        assertEquals(3, count("select count(*) from players where game_id = ?", id));
        assertEquals(1, count("select count(*) from players where user_id = ?", outsider.id()));
    }

    @Test
    void concurrentStartsInitializeExactlyOnce() throws Exception {
        Account host = account();
        int id = lobby(null, host, account(), account());
        assertEquals(List.of(200, 400), race(
            () -> start(id, host).getStatusCode().value(),
            () -> start(id, host).getStatusCode().value()).stream().sorted().toList());
        assertStarted(id, host, 3);
    }

    @Test
    void admissionRacingWithStartLeavesEveryParticipantWithSixCards() throws Exception {
        Account host = account(), fourth = account();
        int id = lobby(null, host, account(), account());
        List<Integer> statuses = race(
            () -> start(id, host).getStatusCode().value(),
            () -> request(HttpMethod.PATCH, "/games/" + id + "/join", fourth, Map.of()).getStatusCode().value());
        assertEquals(200, statuses.get(0));
        assertTrue(statuses.get(1) == 200 || statuses.get(1) == 409);
        assertStarted(id, host, statuses.get(1) == 200 ? 4 : 3);
    }

    @Test
    void concurrentAcceptanceConsumesInvitationOnlyOnce() throws Exception {
        Account host = account(), invitee = account();
        int id = lobby(null, host);
        int invitation = invite(id, host, invitee);
        assertEquals(List.of(200, 404), race(
            () -> accept(invitation, invitee).getStatusCode().value(),
            () -> accept(invitation, invitee).getStatusCode().value()).stream().sorted().toList());
        assertEquals(2, read(id, host).path("players").size());
        assertEquals(1, count("select count(*) from players where game_id = ? and user_id = ?", id, invitee.id()));
    }

    @Test
    void initializationFailureRollsBackDateCardsAndTurnAndPublishesNothing() {
        Account host = account();
        int id = lobby(null, host, account(), account());
        clearInvocations(messagingTemplate);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalArgumentException("Injected failure after saving the deck");
        }).when(cardService).initializeDeck(any(Game.class));
        assertStatus(500, start(id, host));
        assertUnstarted(id, host);
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/game/" + id), any(Game.class));
    }

    @Test
    void insufficientInitialDeckDoesNotLeaveAPartlyStartedGame() {
        Account host = account();
        int id = lobby(null, host, account(), account());
        doReturn(new ArrayList<>()).when(cardService).initializeDeck(any(Game.class));
        assertStatus(400, start(id, host));
        assertUnstarted(id, host);
    }

    @Test
    void creationFailureDoesNotLeaveAnOrphanHostParticipation() {
        Account host = account();
        doThrow(new IllegalArgumentException("Injected failure saving game"))
            .when(gameRepository).save(any(Game.class));
        assertStatus(500, request(HttpMethod.POST, "/games", host, Map.of()));
        assertEquals(1, count("select count(*) from players where user_id = ?", host.id()));
        assertEquals(0, count("select count(*) from games where leader_id = ?", host.id()));
    }

    @Test
    void admissionAndStartNotificationsArePublishedAfterCommit() {
        Account host = account(), third = account();
        int id = lobby(null, host, account());
        clearInvocations(messagingTemplate);
        doAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(messagingTemplate).convertAndSend(eq("/topic/game/" + id), any(Game.class));
        join(id, null, third);
        assertStatus(200, start(id, host));
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/game/" + id), any(Game.class));
    }

    @Test
    void twoRealSockJsSubscribersReceiveJoinsInvitationsAndCommittedStart() throws Exception {
        Account host = account(), third = account(), fourth = account();
        int id = lobby(null, host, account());
        String destination = "/topic/game/" + id;
        // Java's default 8 KiB text buffer is smaller than the existing Game JSON.
        // Browser clients do not have this Java-container limit.
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(512 * 1024);
        WebSocketStompClient stomp = new WebSocketStompClient(new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient(container)))));
        stomp.setInboundMessageSizeLimit(512 * 1024);
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        stomp.start();
        StompSession first = null, second = null;
        try {
            first = stomp.connectAsync("http://127.0.0.1:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
            second = stomp.connectAsync("http://127.0.0.1:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
            BlockingQueue<JsonNode> firstMessages = new LinkedBlockingQueue<>();
            BlockingQueue<JsonNode> secondMessages = new LinkedBlockingQueue<>();
            first.subscribe(destination, frames(firstMessages));
            second.subscribe(destination, frames(secondMessages));
            // Broker echo confirms both subscriptions are installed before changing the lobby.
            String firstProbe = "first-" + UUID.randomUUID();
            String secondProbe = "second-" + UUID.randomUUID();
            first.send(destination, Map.of("probe", firstProbe));
            assertEquals(firstProbe, firstMessages.poll(5, TimeUnit.SECONDS).path("probe").asText());
            second.send(destination, Map.of("probe", secondProbe));
            JsonNode message;
            do { message = secondMessages.poll(5, TimeUnit.SECONDS); assertNotNull(message); }
            while (!secondProbe.equals(message.path("probe").asText()));
            do { message = firstMessages.poll(5, TimeUnit.SECONDS); assertNotNull(message); }
            while (!secondProbe.equals(message.path("probe").asText()));
            firstMessages.clear();
            secondMessages.clear();

            join(id, null, third);
            awaitGame(firstMessages, id, 3, false);
            awaitGame(secondMessages, id, 3, false);
            assertEquals(3, read(id, host).path("players").size());
            int invitation = invite(id, host, fourth);
            assertStatus(200, accept(invitation, fourth));
            awaitGame(firstMessages, id, 4, false);
            awaitGame(secondMessages, id, 4, false);
            assertStatus(200, start(id, host));
            JsonNode firstStart = awaitGame(firstMessages, id, 4, true);
            JsonNode secondStart = awaitGame(secondMessages, id, 4, true);
            JsonNode persisted = assertStarted(id, host, 4);
            assertEquals(persisted.path("startDate"), firstStart.path("startDate"));
            assertEquals(persisted.path("turns").get(0).path("id"), secondStart.path("turns").get(0).path("id"));
        } finally {
            if (first != null && first.isConnected()) first.disconnect();
            if (second != null && second.isConnected()) second.disconnect();
            stomp.stop();
        }
    }

    private StompFrameHandler frames(BlockingQueue<JsonNode> messages) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return JsonNode.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { messages.add((JsonNode) payload); }
        };
    }

    private JsonNode awaitGame(BlockingQueue<JsonNode> messages, int id, int participants, boolean started) throws Exception {
        JsonNode game = messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(game, "Missing lobby notification");
        assertEquals(id, game.path("id").asInt());
        assertEquals(participants, game.path("players").size());
        assertEquals(started, !game.path("startDate").isNull());
        if (started) {
            assertEquals(1, game.path("turns").size());
            for (JsonNode player : game.path("players")) assertEquals(6, player.path("cards").size());
        }
        return game;
    }

    private ResponseEntity<JsonNode> start(int id, Account host) {
        return request(HttpMethod.PATCH, "/games/" + id + "/start", host, null);
    }

    private void assertUnstarted(int id, Account host) {
        JsonNode game = read(id, host);
        assertTrue(game.path("startDate").isNull());
        assertEquals(0, count("select count(*) from turns where game_id = ?", id));
        assertEquals(0, count("select count(*) from cards where game_id = ?", id));
        for (JsonNode player : game.path("players")) assertEquals(0, player.path("cards").size());
    }

    private JsonNode assertStarted(int id, Account host, int participants) {
        JsonNode game = read(id, host);
        assertEquals(participants, game.path("players").size());
        assertFalse(game.path("startDate").isNull());
        assertTrue(game.path("endDate").isNull());
        assertEquals(1, count("select count(*) from turns where game_id = ?", id));
        assertEquals(36, count("select count(*) from cards where game_id = ?", id));
        assertEquals(1, game.path("turns").size());
        JsonNode turn = game.path("turns").get(0);
        assertEquals("WAITING_STORYTELLER_CARD", turn.path("state").asText());
        int storyteller = turn.path("storyteller").path("id").asInt();
        Set<Integer> players = new HashSet<>();
        for (JsonNode player : game.path("players")) {
            players.add(player.path("id").asInt());
            assertEquals(6, player.path("cards").size());
        }
        assertTrue(players.contains(storyteller));
        return game;
    }

    private void finishGame(int id, Account host, Account... others) {
        Map<Integer, Account> accounts = new HashMap<>();
        List<Account> users = new ArrayList<>(List.of(others));
        users.add(host);
        for (Account user : users) {
            accounts.put(jdbc.queryForObject("select id from players where game_id = ? and user_id = ?",
                Integer.class, id, user.id()), user);
        }
        for (int round = 0; round < 6; round++) {
            JsonNode game = read(id, host);
            JsonNode turn = game.path("turns").get(game.path("turns").size() - 1);
            int turnId = turn.path("id").asInt(), storyteller = turn.path("storyteller").path("id").asInt();
            Map<Integer, Integer> cards = new LinkedHashMap<>();
            for (JsonNode player : game.path("players")) {
                cards.put(player.path("id").asInt(), player.path("cards").get(0).path("id").asInt());
            }
            int answer = cards.get(storyteller);
            assertStatus(200, request(HttpMethod.PUT, "/turns/" + turnId + "/answer/" + answer, accounts.get(storyteller), null));
            assertStatus(200, request(HttpMethod.PUT, "/turns/" + turnId + "/clue", accounts.get(storyteller), "clue " + round));
            for (int player : cards.keySet()) if (player != storyteller) {
                assertStatus(200, request(HttpMethod.POST, "/turns/" + turnId + "/play-card/" + cards.get(player) + "/" + player,
                    accounts.get(player), null));
            }
            for (int player : cards.keySet()) if (player != storyteller) {
                assertStatus(200, request(HttpMethod.POST, "/turns/" + turnId + "/vote/" + player + "/" + answer,
                    accounts.get(player), null));
            }
        }
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
