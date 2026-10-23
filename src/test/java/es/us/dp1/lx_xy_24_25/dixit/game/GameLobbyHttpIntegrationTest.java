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
    @Autowired private TurnService turnService;
    @Autowired private javax.sql.DataSource dataSource;
    @Autowired private es.us.dp1.lx_xy_24_25.dixit.statistics.StatisticsRepository statistics;
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
            @Override public void handleFrame(StompHeaders headers, Object payload) { assertFalse(payload.toString().contains("$2a$") || payload.toString().contains("$2b$") || payload.toString().contains("$2y$"), "STOMP must never contain a BCrypt hash"); messages.add((JsonNode) payload); }
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

    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    void completeGamesPreserveHandsHistoryScoresAndImagePriority(int n) {
        List<Account> users = new ArrayList<>();
        for (int i = 0; i < n; i++) users.add(account());
        int id = lobby(null, users.toArray(Account[]::new));
        Account host = users.getFirst();
        ResponseEntity<JsonNode> started = start(id, host);
        assertStatus(200, started);
        assertEquals(36, started.getBody().path("deck").size());
        Map<Integer, Integer> points = new HashMap<>();
        List<Map<String, Object>> history = List.of();
        Set<Integer> initial = new HashSet<>(jdbc.queryForList("select id from cards where game_id = ?", Integer.class, id));
        boolean reused = false;
        for (int round = 1; round <= n * 2; round++) {
            JsonNode before = read(id, host);
            assertEquals(round, before.path("turns").size());
            for (JsonNode p : before.path("players")) assertEquals(6, p.path("cards").size());
            assertActiveImagesUnique(id, n * 6);
            VotingRound voting = prepareVotingRound(id, users);
            Set<Integer> unused = cardIds(id, "player_id is null and turn_id is null");
            Set<Integer> oldHands = cardIds(id, "player_id is not null and turn_id is null");
            assertActiveImagesUnique(id, n * 6);
            List<Integer> voters = new ArrayList<>(voting.voters().keySet());
            Map<Integer, Integer> targets = new HashMap<>();
            for (int i = 0; i < voters.size(); i++) {
                int target = round % 3 == 0 ? voting.cards().get(voters.get((i + 1) % voters.size()))
                    : round % 3 == 1 || i == 0 ? voting.cards().get(voting.storyteller()) : voting.cards().get(voters.getFirst());
                targets.put(voters.get(i), target);
                assertStatus(200, vote(voting, voters.get(i), target));
            }
            long correct = targets.values().stream().filter(c -> c.equals(voting.cards().get(voting.storyteller()))).count();
            for (int player : voting.cards().keySet()) {
                int delta = correct == 0 || correct == voters.size() ? (player == voting.storyteller() ? 0 : 2)
                    : (player == voting.storyteller() || targets.get(player).equals(voting.cards().get(voting.storyteller())) ? 3 : 0);
                if (player != voting.storyteller()) delta += (int) targets.values().stream().filter(c -> c.equals(voting.cards().get(player))).count();
                points.merge(player, delta, Integer::sum);
                assertEquals(points.get(player), jdbc.queryForObject("select points from players where id = ?", Integer.class, player));
            }
            List<Map<String, Object>> reloaded = playedHistory(id);
            assertEquals(history, reloaded.stream().filter(row -> ((Number) row.get("TURN_ID")).intValue() != voting.turnId()).toList());
            history = reloaded;
            assertEquals(round * n, count("select count(*) from cards where game_id = ? and turn_id is not null and user_id is not null and player_id is null", id));
            assertEquals(round * (n - 1), count("select count(*) from player_votes v join cards c on c.id = v.card_id where c.game_id = ?", id));
            JsonNode after = read(id, host);
            if (round < n * 2) {
                assertEquals(round + 1, after.path("turns").size());
                Set<Integer> dealt = cardIds(id, "player_id is not null and turn_id is null");
                dealt.removeAll(oldHands);
                assertEquals(n, dealt.size());
                assertEquals(Math.min(unused.size(), n), dealt.stream().filter(unused::contains).count());
                for (Integer card : dealt) if (!initial.contains(card)) {
                    reused = true;
                    assertEquals(1, count("select count(*) from cards c where c.id = ? and c.turn_id is null and c.user_id is null and exists (select 1 from cards h where h.game_id = c.game_id and h.image_id = c.image_id and h.turn_id is not null and h.id <> c.id)", card));
                    assertEquals(0, count("select count(*) from player_votes where card_id = ?", card));
                }
                for (JsonNode p : after.path("players")) assertEquals(6, p.path("cards").size());
            } else {
                assertFalse(after.path("endDate").isNull()); assertFalse(after.path("winners").isEmpty());
                for (JsonNode p : after.path("players")) assertEquals(5, p.path("cards").size());
            }
            assertEquals(n == 4 ? 36 + 4 * Math.max(0, Math.min(round, 7) - 3) : 36, count("select count(*) from cards where game_id = ?", id));
        }
        assertEquals(n == 4, reused);
        assertEquals(history, playedHistory(id));
        for (Account user : users) {
            assertNotNull(statistics.findMostPlayedCardByUserId(user.id()));
            assertEquals(points.get(jdbc.queryForObject("select id from players where game_id = ? and user_id = ?", Integer.class, id, user.id())).longValue(), statistics.totalPointsByUserId(user.id()));
            assertEquals(1, count("select count(*) from achievements where user_id = ? and threshold = 1 and dtype = 'PlayAchievement' and date_obtained is not null", user.id()));
            int trickVotes = count("select count(*) from player_votes v join cards c on c.id = v.card_id join turns t on t.id = c.turn_id join players s on s.id = t.storyteller_id where c.game_id = ? and c.user_id = ? and s.user_id <> c.user_id", id, user.id());
            for (Map<String, Object> a : jdbc.queryForList("select threshold, date_obtained from achievements where user_id = ? and dtype = 'TrickAchievement'", user.id()))
                assertEquals(trickVotes >= ((Number) a.get("THRESHOLD")).intValue(), a.get("DATE_OBTAINED") != null);
        }
    }

    @Test
    void replenishmentRollbackDoesNotPublishOrLeavePartialHistoryAndRetryIsSafe() {
        List<Account> users = List.of(account(), account(), account(), account());
        int id = lobby(null, users.toArray(Account[]::new));
        assertStatus(200, start(id, users.getFirst()));
        for (int i = 0; i < 3; i++) finishVoting(prepareVotingRound(id, users));
        VotingRound voting = prepareVotingRound(id, users);
        List<Integer> voters = new ArrayList<>(voting.voters().keySet());
        for (int i = 0; i < voters.size() - 1; i++) assertStatus(200, vote(voting, voters.get(i), voting.cards().get(voting.storyteller())));
        List<Map<String, Object>> before = databaseState(id);
        clearInvocations(messagingTemplate);
        doAnswer(inv -> { inv.callRealMethod(); throw new IllegalStateException("Injected after replenishment"); }).when(cardService).replenishHands(any(Game.class));
        int last = voters.getLast();
        assertStatus(500, vote(voting, last, voting.cards().get(voting.storyteller())));
        assertEquals(before, databaseState(id));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        doCallRealMethod().when(cardService).replenishHands(any(Game.class));
        assertStatus(200, vote(voting, last, voting.cards().get(voting.storyteller())));
        assertEquals(40, count("select count(*) from cards where game_id = ?", id));
        List<Map<String, Object>> committed = databaseState(id);
        turnService.moveToNextTurn(turnService.findById(voting.turnId()));
        turnService.moveToNextTurn(turnService.findById(voting.turnId()));
        assertEquals(committed, databaseState(id));
        assertFalse(vote(voting, last, voting.cards().get(voting.storyteller())).getStatusCode().is2xxSuccessful());
        assertEquals(committed, databaseState(id));
    }

    @Test
    void concurrentFinalVotesReplenishOnlyOnce() throws Exception {
        List<Account> users = List.of(account(), account(), account(), account());
        int id = lobby(null, users.toArray(Account[]::new));
        assertStatus(200, start(id, users.getFirst()));
        for (int i = 0; i < 3; i++) finishVoting(prepareVotingRound(id, users));
        VotingRound voting = prepareVotingRound(id, users);
        List<Integer> voters = new ArrayList<>(voting.voters().keySet());
        int answer = voting.cards().get(voting.storyteller());
        assertStatus(200, vote(voting, voters.getFirst(), answer));
        assertEquals(List.of(200, 200), race(() -> vote(voting, voters.get(1), answer).getStatusCode().value(), () -> vote(voting, voters.get(2), answer).getStatusCode().value()));
        assertEquals(5, count("select count(*) from turns where game_id = ?", id));
        assertEquals(40, count("select count(*) from cards where game_id = ?", id));
        assertEquals(3, count("select count(*) from player_votes v join cards c on c.id = v.card_id where c.turn_id = ?", voting.turnId()));
        assertActiveImagesUnique(id, 24);
        for (JsonNode p : read(id, users.getFirst()).path("players")) assertEquals(6, p.path("cards").size());
    }

    @Test
    void realStompReceivesReplenishedHandsOnlyAfterDatabaseCommit() throws Exception {
        List<Account> users = List.of(account(), account(), account(), account());
        int id = lobby(null, users.toArray(Account[]::new));
        assertStatus(200, start(id, users.getFirst()));
        for (int i = 0; i < 3; i++) finishVoting(prepareVotingRound(id, users));
        VotingRound voting = prepareVotingRound(id, users);
        String destination = "/topic/game/" + id;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(512 * 1024);
        WebSocketStompClient stomp = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient(container)))));
        stomp.setInboundMessageSizeLimit(512 * 1024);
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        stomp.start();
        StompSession session = null;
        try {
            session = stomp.connectAsync("http://127.0.0.1:" + port + "/ws", new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
            session.subscribe(destination, frames(messages));
            session.send(destination, Map.of("probe", "ready"));
            JsonNode probe = messages.poll(5, TimeUnit.SECONDS);
            assertNotNull(probe); assertEquals("ready", probe.path("probe").asText());
            doAnswer(inv -> {
                try (java.sql.Connection connection = dataSource.getConnection();
                     java.sql.PreparedStatement statement = connection.prepareStatement("select count(*) from turns where game_id = ?")) {
                    statement.setInt(1, id);
                    try (java.sql.ResultSet result = statement.executeQuery()) { assertTrue(result.next()); assertEquals(5, result.getInt(1)); }
                }
                return inv.callRealMethod();
            }).when(messagingTemplate).convertAndSend(eq(destination), any(JsonNode.class));
            finishVoting(voting);
            JsonNode event = messages.poll(5, TimeUnit.SECONDS);
            assertNotNull(event); assertEquals(5, event.path("turns").size()); assertEquals(40, event.path("deck").size());
            for (JsonNode p : event.path("players")) assertEquals(6, p.path("cards").size());
            assertEquals(read(id, users.getFirst()), event, "Event must represent durable REST state");
        } finally {
            if (session != null && session.isConnected()) session.disconnect();
            stomp.stop();
        }
    }

    private record VotingRound(int gameId, int turnId, int storyteller, Map<Integer, Integer> cards, LinkedHashMap<Integer, Account> voters) {}

    private VotingRound prepareVotingRound(int id, List<Account> users) {
        JsonNode game = read(id, users.getFirst());
        JsonNode turn = game.path("turns").get(game.path("turns").size() - 1);
        int tid = turn.path("id").asInt(), storyteller = turn.path("storyteller").path("id").asInt();
        Map<Integer, Integer> cards = new LinkedHashMap<>();
        LinkedHashMap<Integer, Account> voters = new LinkedHashMap<>();
        Map<Integer, Account> accounts = new HashMap<>();
        for (Account u : users) accounts.put(jdbc.queryForObject("select id from players where game_id = ? and user_id = ?", Integer.class, id, u.id()), u);
        for (JsonNode p : game.path("players")) {
            int pid = p.path("id").asInt(); cards.put(pid, p.path("cards").get(0).path("id").asInt());
            if (pid != storyteller) voters.put(pid, accounts.get(pid));
        }
        assertStatus(200, request(HttpMethod.PUT, "/turns/" + tid + "/answer/" + cards.get(storyteller), accounts.get(storyteller), null));
        assertStatus(200, request(HttpMethod.PUT, "/turns/" + tid + "/clue", accounts.get(storyteller), "replenishment clue"));
        for (int pid : voters.keySet()) assertStatus(200, request(HttpMethod.POST, "/turns/" + tid + "/play-card/" + cards.get(pid) + "/" + pid, accounts.get(pid), null));
        return new VotingRound(id, tid, storyteller, cards, voters);
    }

    private ResponseEntity<JsonNode> vote(VotingRound r, int voter, int card) {
        return request(HttpMethod.POST, "/turns/" + r.turnId() + "/vote/" + voter + "/" + card, r.voters().get(voter), null);
    }

    private void finishVoting(VotingRound r) {
        for (int voter : r.voters().keySet()) assertStatus(200, vote(r, voter, r.cards().get(r.storyteller())));
    }

    private Set<Integer> cardIds(int id, String predicate) {
        return new HashSet<>(jdbc.queryForList("select id from cards where game_id = ? and " + predicate, Integer.class, id));
    }

    private void assertActiveImagesUnique(int id, int expected) {
        String predicate = " from cards c where c.game_id = ? and (c.player_id is not null or exists (select 1 from turns t where t.id = c.turn_id and t.state <> 'FINISHED'))";
        assertEquals(expected, count("select count(*)" + predicate, id)); assertEquals(expected, count("select count(distinct image_id)" + predicate, id));
    }

    private List<Map<String, Object>> playedHistory(int id) {
        return jdbc.queryForList("select c.id, c.image_id, c.turn_id, c.user_id, c.player_id, v.player_id as voter_id from cards c left join player_votes v on v.card_id = c.id where c.game_id = ? and c.turn_id is not null order by c.id, v.player_id", id);
    }

    private List<Map<String, Object>> databaseState(int id) {
        List<Map<String, Object>> state = new ArrayList<>(jdbc.queryForList("select * from cards where game_id = ? order by id", id));
        state.addAll(jdbc.queryForList("select * from turns where game_id = ? order by id", id));
        state.addAll(jdbc.queryForList("select id, points from players where game_id = ? order by id", id));
        state.addAll(jdbc.queryForList("select v.* from player_votes v join cards c on c.id = v.card_id where c.game_id = ? order by card_id, v.player_id", id));
        return state;
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
        if (count("select count(*) from friend_requests where ((sender_id=? and receiver_id=?) or (sender_id=? and receiver_id=?)) and date_accepted is not null", host.id(), invitee.id(), invitee.id(), host.id()) == 0) {
            assertStatus(200, request(HttpMethod.POST, "/friends/addFriend/" + invitee.id(), host, null));
            int friendship = count("select id from friend_requests where sender_id=? and receiver_id=?", host.id(), invitee.id());
            assertStatus(200, request(HttpMethod.PATCH, "/friends/" + friendship + "/accept", invitee, null));
        }

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
