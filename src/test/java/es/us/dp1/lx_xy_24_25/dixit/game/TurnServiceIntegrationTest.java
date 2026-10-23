// TODO: fix

package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.achievement.PlayAchievement;
import es.us.dp1.lx_xy_24_25.dixit.auth.AuthService;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.request.SignupRequest;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.card.CardRepository;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerRepository;
import es.us.dp1.lx_xy_24_25.dixit.user.Authorities;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesRepository;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TurnServiceIntegrationTest {

    @Autowired
    private TurnService turnService;

    @Autowired
    private TurnRepository turnRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthoritiesRepository authoritiesRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AchievementService achievementService;

    @Test
    void registeredPlayersCanFinishGameOnLastVote() throws Exception {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SignupRequest request = new SignupRequest();
            request.setUsername("finish-registered-" + i);
            request.setPassword("password");
            request.setAuthority("PLAYER");
            authService.createUser(request);
            users.add(userRepository.findByUsername(request.getUsername()).orElseThrow());
        }
        // Reload the association created by signup, including its player without a game.
        entityManager.flush();
        entityManager.clear();
        users = users.stream().map(u -> userRepository.findById(u.getId()).orElseThrow()).toList();
        for (User user : users) {
            assertEquals(1, user.getPlayers().size());
            assertNull(user.getPlayers().getFirst().getGame());
            achievementService.initializePlayAchievements(user);
        }

        Game game = gameService.createGame(null, users.getFirst());
        gameService.addPlayerToGame(game, users.get(1));
        gameService.addPlayerToGame(game, users.get(2));
        game.setStartDate(new java.util.Date());
        turnService.initializeFirstTurn(game);
        Map<Integer, Integer> expectedPoints = new HashMap<>();
        for (Player player : game.getPlayers()) {
            expectedPoints.put(player.getId(), 0);
        }

        for (int i = 0; i < 6; i++) {
            Turn turn = game.getTurns().getLast();
            Player storyteller = turn.getStoryteller();
            List<Player> voters = turn.getNonStorytellers();
            Card answer = storyteller.getCards().getFirst();
            Card playerCard = voters.getFirst().getCards().getFirst();
            turnService.setStorytellerAnswer(turn, answer);
            turnService.setStorytellerClue(turn, "clue " + i);
            turnService.playerPlayCard(turn, voters.getFirst(), playerCard);
            turnService.playerPlayCard(turn, voters.get(1), voters.get(1).getCards().getFirst());
            turnService.playerVote(turn, voters.getFirst(), answer);
            assertNull(game.getEndDate(), "The game must not finish before the last vote");
            for (User user : users) {
                achievementService.checkPlayAchievements(user);
                assertTrue(achievementService.findAllAchievementsByUserId(user.getId()).stream()
                    .allMatch(a -> a.getDateObtained() == null), "An ongoing game must not unlock PLAY achievements");
            }
            turnService.playerVote(turn, voters.get(1), answer);
            expectedPoints.merge(voters.getFirst().getId(), 2, Integer::sum);
            expectedPoints.merge(voters.get(1).getId(), 2, Integer::sum);
            assertEquals(TurnState.FINISHED, turn.getState());
        }

        Integer gameId = game.getId();
        entityManager.flush();
        entityManager.clear();
        Game finished = gameRepository.findById(gameId).orElseThrow();
        assertNotNull(finished.getEndDate());
        assertEquals(6, finished.getTurns().size());
        assertEquals(2, finished.getTurns().getLast().getPlayedCards().stream()
            .mapToInt(c -> c.getVotes().size()).sum());
        assertEquals(3, finished.getWinners().size(), "Equal scores must preserve tied winners");
        for (Player player : finished.getPlayers()) {
            assertEquals(expectedPoints.get(player.getId()), player.getPoints());
            List<PlayAchievement> playAchievements = achievementService.findAllAchievementsByUserId(player.getUser().getId())
                .stream().filter(PlayAchievement.class::isInstance).map(PlayAchievement.class::cast).toList();
            assertEquals(4, playAchievements.size());
            for (PlayAchievement achievement : playAchievements) {
                if (achievement.getThreshold() == 1) {
                    assertNotNull(achievement.getDateObtained(), "The last vote must unlock the first completed game");
                } else {
                    assertNull(achievement.getDateObtained(), "The signup participation must not count as a game");
                }
            }
        }
        for (Turn turn : finished.getTurns()) {
            for (Card card : turn.getPlayedCards()) {
                assertNull(card.getOwner());
                assertNotNull(card.getUser(), "Scored cards must preserve who played them");
            }
        }
    }

    private int userSequence = 0;

//     -------------------Helpers to create entities -------------------
    private Player createPlayer() {
        return createPlayer(null);
    }

    private Player createPlayer(Game game) {
        return createPlayer(game, "player");
    }

    private Player createPlayer(Game game, String prefix) {
        Player player = new Player();
        player.setPoints(0);
        player.setGame(game);
        player.setUser(createUser(prefix));
        return playerRepository.save(player);
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "-" + userSequence++);
        user.setPassword("password");
        user.setAuthority(resolveAuthority("PLAYER"));
        return userRepository.save(user);
    }

    private Authorities resolveAuthority(String authority) {
        return authoritiesRepository.findByName(authority)
            .orElseGet(() -> {
                Authorities auth = new Authorities();
                auth.setAuthority(authority);
                return authoritiesRepository.save(auth);
            });
    }

//     ------------------- Tests -------------------


    @Test
    public void testSetStorytellerAnswer() {
//         Create and save a Card
        Card card = new Card();
        card = cardRepository.save(card);

        Game game = new Game();
        gameService.saveGame(game);
//         Create and save a Turn
        Turn turn = new Turn();
        turn.setGame(game);
        turn = turnRepository.save(turn);

//         Call service
        Turn updated = turnService.setStorytellerAnswer(turn, card);

//         Assertions
        assertNotNull(updated);
        assertNotNull(updated.getAnswer(), "Answer should be set on the turn");
        assertEquals(card.getId(), updated.getAnswer().getId(), "Answer card id should match");

//        State should change to waiting storyteller clue
        TurnState newState = TurnState.WAITING_STORYTELLER_CLUE;
        assertEquals(newState, updated.getState(), "Turn state should be updated to WAITING_STORYTELLER_CLUE");

    }

    @Test
    public void testSetStorytellerClue() {
        Game game = new Game();
        gameService.saveGame(game);
//         Create and save a Turn
        Turn turn = new Turn();
        turn.setGame(game);
        turn = turnRepository.save(turn);

//         Set a clue
        String clue = "This is a test clue";
        Turn updated = turnService.setStorytellerClue(turn, clue);

//         Assertions
        assertNotNull(updated);
        assertEquals(clue, updated.getClue(), "Clue should be set on the turn");
        assertEquals(TurnState.WAITING_PLAYER_CARDS, updated.getState(), "Turn state should be updated to WAITING_PLAYER_CARDS");

    }

    @Test
    public void testPlayerPlayCard() {
//         Create and save a Player
        Player player = createPlayer();

//         Create and save a Card with owner set to the player
        Card card = new Card();
        card.setOwner(player);
        card = cardRepository.save(card);

        Game game = new Game();
        gameService.saveGame(game);
//         Create and save a Turn
        Turn turn = new Turn();
        turn.setGame(game);
        turn = turnRepository.save(turn);

//         Call service to player play card
        Card playedCard = turnService.playerPlayCard(turn, player, card);

//         Assertions
        assertNotNull(playedCard);
        assertNotNull(playedCard.getTurn(), "Card's turn should be set");
        assertEquals(turn.getId(), playedCard.getTurn().getId(), "Card's turn id should match");

//         Verify card is in turn's played cards
        Turn reloadedTurn = turnRepository.findById(turn.getId()).orElseThrow();
        assertTrue(reloadedTurn.getPlayedCards().contains(playedCard), "Card should be in turn's played cards list");
    }

    @Test
    public void testAutomaticTransitionToShowingCards() {
//         Create a game with 3 players (1 storyteller + 2 other players)
        Game game = new Game();
        game = gameRepository.save(game);

 //        Create 3 players
        Player storyteller = createPlayer(game);
        game.getPlayers().add(storyteller);

        Player player1 = createPlayer(game);
        game.getPlayers().add(player1);

        Player player2 = createPlayer(game);
        game.getPlayers().add(player2);

        game = gameRepository.save(game);

//         Create a turn for this game with storyteller
        Turn turn = new Turn();
        turn.setGame(game);
        turn.setStoryteller(storyteller);
        turn = turnRepository.save(turn);

//        Create 2 cards, one for each player (not storyteller)
        Card card1 = new Card();
        card1.setOwner(player1);
        card1 = cardRepository.save(card1);

        Card card2 = new Card();
        card2.setOwner(player2);
        card2 = cardRepository.save(card2);

        Card card3 = new Card();
        card3.setOwner(storyteller);
        card3 = cardRepository.save(card3);

        turnService.setStorytellerAnswer(turn, card3);
        turnService.setStorytellerClue(turn, "This is a test");

//         Player 1 plays card
        turnService.playerPlayCard(turn, player1, card1);
        Turn turnAfterFirstCard = turnRepository.findById(turn.getId()).orElseThrow();
//         State should be WAITING_PLAYER_CARDS (only 1 of 2 has played)
        assertEquals(TurnState.WAITING_PLAYER_CARDS, turnAfterFirstCard.getState(), "State should not change after first player plays");

//         Player 2 plays card
        turnService.playerPlayCard(turn, player2, card2);
        Turn turnAfterAllCards = turnRepository.findById(turn.getId()).orElseThrow();
//         State should now be SHOWING_CARDS (all 2 non-storyteller players have played)
        assertEquals(TurnState.SHOWING_CARDS, turnAfterAllCards.getState(), "State should change to SHOWING_CARDS when all players have played");
        assertEquals(3, turnAfterAllCards.getPlayedCards().size(), "Should have 2 played cards");
    }

//     Helper to create a voting turn with specified number of non-storyteller players

    private ScoringContext createVotingTurnWithPlayers(int nonStoryPlayers) {
        Game game = gameRepository.save(new Game());

        Player storyteller = createPlayer(game);
        game.getPlayers().add(storyteller);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < nonStoryPlayers; i++) {
            Player player = createPlayer(game);
            players.add(player);
            game.getPlayers().add(player);
        }

        game = gameRepository.save(game);

        game.setStartDate(new java.util.Date());
        turnService.initializeFirstTurn(game);
        Turn turn = game.getTurns().getFirst();
        turn.setStoryteller(storyteller);
        Card storytellerCard = storyteller.getCards().getFirst();
        turnService.setStorytellerAnswer(turn, storytellerCard);
        turnService.setStorytellerClue(turn, "hint");
        Map<Integer, Card> cardsByPlayer = new HashMap<>();
        for (Player player : players) {
            Card played = turnService.playerPlayCard(turn, player, player.getCards().getFirst());
            cardsByPlayer.put(player.getId(), played);
        }

        Turn votingTurn = turnRepository.findById(turn.getId()).orElseThrow();

        votingTurn.setState(TurnState.VOTING);
        votingTurn = turnRepository.save(votingTurn);

        Turn refreshedTurn = turnRepository.findById(turn.getId()).orElseThrow();

        return new ScoringContext(refreshedTurn, storyteller, players, storytellerCard, cardsByPlayer);
    }

    private record ScoringContext(Turn turn, Player storyteller, List<Player> nonStoryPlayers,
                                  Card storytellerCard, Map<Integer, Card> playerCards) {

    }

//    scoring tests


    @Test //no player guesses storyteller
    public void testScoreTurnWhenNoPlayerGuessesStoryteller() {
        ScoringContext context = createVotingTurnWithPlayers(2);

        Player player1 = context.nonStoryPlayers().get(0);
        Player player2 = context.nonStoryPlayers().get(1);

        Card cardPlayer1 = context.playerCards().get(player1.getId());
        Card cardPlayer2 = context.playerCards().get(player2.getId());

        turnService.playerVote(context.turn(), player1, cardPlayer2);
        turnService.playerVote(context.turn(), player2, cardPlayer1);

        Player reloadedPlayer1 = playerRepository.findById(player1.getId()).orElseThrow();
        Player reloadedPlayer2 = playerRepository.findById(player2.getId()).orElseThrow();
        Player reloadedStoryteller = playerRepository.findById(context.storyteller().getId()).orElseThrow();

        assertEquals(3, reloadedPlayer1.getPoints(), "Player1 should gain 1 fooling point plus 2 consolation points");
        assertEquals(3, reloadedPlayer2.getPoints(), "Player2 should gain 1 fooling point plus 2 consolation points");
        assertEquals(0, reloadedStoryteller.getPoints(), "Storyteller gets 0 when nobody guesses their card");
        assertEquals(TurnState.FINISHED, context.turn().getState(), "Turn should be marked as finished after scoring");
    }

    @Test //all players guess storyteller
    public void testScoreTurnWhenAllPlayersGuessStoryteller() {
        ScoringContext context = createVotingTurnWithPlayers(3);

        for (Player player : context.nonStoryPlayers()) {
            turnService.playerVote(context.turn(), player, context.storytellerCard());
        }

        for (Player player : context.nonStoryPlayers()) {
            Player reloadedPlayer = playerRepository.findById(player.getId()).orElseThrow();
            assertEquals(2, reloadedPlayer.getPoints(), "All guessers only receive the 2 consolation points when everyone succeeds");
        }

        Player reloadedStoryteller = playerRepository.findById(context.storyteller().getId()).orElseThrow();
        assertEquals(0, reloadedStoryteller.getPoints(), "Storyteller does not score when everybody guesses right");
        assertEquals(TurnState.FINISHED, context.turn().getState(), "Turn should be marked as finished after scoring");
    }

    @Test // some players guess storyteller
    public void testScoreTurnWhenOnlySomePlayersGuessStoryteller() {
        ScoringContext context = createVotingTurnWithPlayers(2);

        Player correctGuesser = context.nonStoryPlayers().get(0);
        Player misledPlayer = context.nonStoryPlayers().get(1);
        Card cardFromGuesser = context.playerCards().get(correctGuesser.getId());

        turnService.playerVote(context.turn(), correctGuesser, context.storytellerCard());
        turnService.playerVote(context.turn(), misledPlayer, cardFromGuesser);

        Player reloadedGuesser = playerRepository.findById(correctGuesser.getId()).orElseThrow();
        Player reloadedMisled = playerRepository.findById(misledPlayer.getId()).orElseThrow();
        Player reloadedStoryteller = playerRepository.findById(context.storyteller().getId()).orElseThrow();

        assertEquals(4, reloadedGuesser.getPoints(), "Correct guess (3) plus attracting a vote (1) should total 4");
        assertEquals(0, reloadedMisled.getPoints(), "Wrong voters should not earn any points");
        assertEquals(3, reloadedStoryteller.getPoints(), "Storyteller should score 3 when only some players guess correctly");
        assertEquals(TurnState.FINISHED, context.turn().getState(), "Turn should be marked as finished after scoring");
    }

    @Test
    public void testMoveToNextTurn() {
        ScoringContext context = createVotingTurnWithPlayers(2);
        Turn currentTurn = context.turn();
        Game game = currentTurn.getGame();
        Player expectedNext = game.nextStoryteller(context.storyteller());
        for (Player voter : context.nonStoryPlayers()) currentTurn.registerVote(voter, context.storytellerCard());
        turnService.scoreTurn(currentTurn);
        turnService.moveToNextTurn(currentTurn);
        Turn newTurn = game.getTurns().getLast();
        assertNotEquals(currentTurn.getId(), newTurn.getId());
        assertEquals(game.getId(), newTurn.getGame().getId());
        assertEquals(expectedNext.getId(), newTurn.getStoryteller().getId());
        assertEquals(TurnState.WAITING_STORYTELLER_CARD, newTurn.getState());
        assertTrue(newTurn.getPlayedCards().isEmpty());
        for (Player player : game.getPlayers()) assertEquals(6, player.getCards().size(), "Every hand must be replenished to six");
        assertEquals(18, cardRepository.countByGameAndOwnerIsNull(game), "15 unused plus 3 historical cards");
        turnService.moveToNextTurn(currentTurn);
        assertEquals(2, game.getTurns().size(), "Retry must not create another successor");
        assertTrue(game.getPlayers().stream().allMatch(p -> p.getCards().size() == 6));
    }

    @Test
    public void testInitializeFirstTurn() {
//         Create a game
        Game game = new Game();
        game = gameRepository.save(game);

//         Create a storyteller and two players
        Player storyteller = createPlayer(game);
        game.getPlayers().add(storyteller);
        Player player1 = createPlayer(game);
        game.getPlayers().add(player1);
        Player player2 = createPlayer(game);
        game.getPlayers().add(player2);
        game = gameRepository.save(game);

//         Call service to initialize first turn
         turnService.initializeFirstTurn(game);
         Turn firstTurn = game.getTurns().getFirst();

//         Assertions
        assertNotNull(firstTurn);
        assertEquals(game.getId(), firstTurn.getGame().getId(), "First turn should belong to the correct game");
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getId().equals(firstTurn.getStoryteller().getId())), "Storyteller should be one of the game's players");
        assertEquals(TurnState.WAITING_STORYTELLER_CARD, firstTurn.getState(), "First turn should be in WAITING_STORYTELLER_CARD state");

        for (Player player : game.getPlayers()) {
            assertEquals(6, player.getCards().size(), "Each player should have received six cards");
            for (Card card : player.getCards()) {
                assertEquals(player.getId(), card.getOwner().getId(), "Each card should be owned by the correct player");
            }
        }
        Integer remainingCards = cardRepository.countByGameAndOwnerIsNull(game);
        assertEquals(36 - (6 * game.getPlayers().size()), remainingCards, "There should be correct number of cards left in the deck");
    }


}
