package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.PlayerAlreadyInGameException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.TooManyPlayersException;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerService;
import es.us.dp1.lx_xy_24_25.dixit.social.FriendRequest;
import es.us.dp1.lx_xy_24_25.dixit.social.FriendRequestService;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Epic("Games")
class GameServiceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private GameService gameService;
    @Autowired
    private AuthoritiesService authoritiesService;
    @Autowired
    private FriendRequestService friendRequestService;
    @Autowired
    private PlayerService playerService;

    private User user0;
    private User user1;
    private User user2;
    private User user3;
    private User user4;
    

    @BeforeEach
    void setUp() {
        user0 = new User();
        user0.setUsername("user0");
        user0.setPassword("password");
        user0.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user0);

        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password");
        user2.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user2);

        user3 = new User();
        user3.setUsername("user3");
        user3.setPassword("password");
        user3.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user3);

        user4 = new User();
        user4.setUsername("user4");
        user4.setPassword("password");
        user4.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user4);
    }

    @Test
    @Transactional
    @DisplayName("Should find game won by user")
    @Description("Finds the games where user0 user is in the winners list")
    void findGamesByWinnerUsername() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);
        gameService.addPlayerToGame(game, user2);

        Player winner = userService.findByUsername("user0").getPlayers().getFirst();
        game.getWinners().add(winner);
        gameService.saveGame(game);

        List<Game> games = gameService.findGamesByWinnerUsername("user0");
        assertEquals(1, games.size());
        assertEquals(game.getId(), games.getFirst().getId());
    }

    @Test
    @Transactional
    @DisplayName("Should find the games that have finished")
    void findFinished() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game wrongGame = gameService.createGame("", user0);
        gameService.addPlayerToGame(wrongGame, user1);
        gameService.addPlayerToGame(wrongGame, user2);

        Game rightGame = gameService.createGame("", user0);
        gameService.addPlayerToGame(rightGame, user1);
        gameService.addPlayerToGame(rightGame, user2);

        rightGame.setStartDate(new Date());
        rightGame.setEndDate(new Date());
        gameService.saveGame(rightGame);

        List<Game> games = gameService.findFinished();
        assertEquals(1, games.size());
        assertEquals(rightGame.getId(), games.getFirst().getId());
    }

    @Test
    @Transactional
    @DisplayName("Should find games that are being played")
    void findPlaying() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game wrongGame = gameService.createGame("", user0);
        gameService.addPlayerToGame(wrongGame, user1);
        gameService.addPlayerToGame(wrongGame, user2);

        Game rightGame = gameService.createGame("", user0);
        gameService.addPlayerToGame(rightGame, user1);
        gameService.addPlayerToGame(rightGame, user2);

        rightGame.setStartDate(new Date());

        wrongGame.setStartDate(new Date());
        wrongGame.setEndDate(new Date());
        gameService.saveGame(wrongGame);

        List<Game> games = gameService.findPlaying();
        assertEquals(1, games.size());
        assertEquals(rightGame.getId(), games.getFirst().getId());
    }

    @Test
    @Transactional
    @DisplayName("Checks that a game can't start if there aren't enough players")
    @Description("Checks that an exception is thrown when trying to start a game with less than the minimum amount of players")
    void minPlayersBeforeStart() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);
        assertThrows(IllegalStateException.class, () -> game.setStartDate(new Date()));
    }

    @Test
    @Transactional
    @DisplayName("Checks that a game can't be set as finished if it hasn't started")
    void gameMustStartBeforeEnd() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);
        gameService.addPlayerToGame(game, user2);
        assertThrows(IllegalStateException.class, () -> game.setEndDate(new Date()));
    }

    @Test
    @Transactional
    void findByIdIfUserInGame_variasComprobaciones() throws TooManyPlayersException, PlayerAlreadyInGameException {

        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);

        //1)Usuario participante
        Game foundGame1 = gameService.findByIdIfUserInGame(game.getId(), user1);
        assertNotNull(foundGame1);
        assertEquals(game.getId(), foundGame1.getId());

        //2)Usuario que no participa
        assertThrows(AccessDeniedException.class,
            () -> gameService.findByIdIfUserInGame(game.getId(), user2) );

        //3)Juego inexistente
        int nonexistentGameId = 99999;
        assertThrows(ResourceNotFoundException.class,
            () -> gameService.findByIdIfUserInGame(nonexistentGameId, user1) );

    }

    @Test
    @Transactional
    @DisplayName("Checks that the game is automatically finished")
    @Description("Should end a game after the maximum number of turns is reached")
    public void endGameIfNeeded() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);
        gameService.addPlayerToGame(game, user2);
        gameService.addPlayerToGame(game, user3);

        game.setStartDate(new Date());

        // Add turns to reach the maximum number of turns
        int maxTurns = game.getPlayers().size() * 2;
        for (int i = 0; i < maxTurns - 1; i++) {
            game.getTurns().add(new Turn());
        }

        Turn votingTurn = new Turn();
        Player storyteller = game.getPlayers().getFirst();
        votingTurn.setStoryteller(storyteller);
        votingTurn.setGame(game);

        Card card = new Card();
        game.playersExcept(storyteller).forEach(card::vote);
        votingTurn.getPlayedCards().add(card);
        game.getTurns().add(votingTurn);

        gameService.endGameIfNeeded(game, votingTurn);

        assertNotNull(game.getEndDate(), "The game should have ended and endDate should be set.");
    }

    @Test
    @Transactional
    @DisplayName("Should throw TooManyPlayers exception")
    public void checkThrowsTooManyPlayersException() throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game = gameService.createGame("", user0);
        gameService.addPlayerToGame(game, user1);
        gameService.addPlayerToGame(game, user2);
        gameService.addPlayerToGame(game, user3);
        assertThrows(TooManyPlayersException.class, () -> gameService.addPlayerToGame(game, user4));
    }

    @Test
    @Transactional
    public void addSpectatorsToGame() throws PlayerAlreadyInGameException, TooManyPlayersException {
        Game game = gameService.createGame("", user1);
        gameService.addPlayerToGame(game, user2);
        gameService.addPlayerToGame(game, user3);
        game.setStartDate(new Date());

        FriendRequest friendRequest1 = new FriendRequest();
        friendRequest1.setSender(user1);
        friendRequest1.setReceiver(user0);
        friendRequest1.setDateAccepted(new Date());
        friendRequestService.save(friendRequest1);

        FriendRequest friendRequest2 = new FriendRequest();
        friendRequest2.setSender(user2);
        friendRequest2.setReceiver(user0);
        friendRequest2.setDateAccepted(new Date());
        friendRequestService.save(friendRequest2);

        FriendRequest friendRequest3 = new FriendRequest();
        friendRequest3.setSender(user3);
        friendRequest3.setReceiver(user0);
        friendRequest3.setDateAccepted(new Date());
        friendRequestService.save(friendRequest3);

        FriendRequest friendRequest4 = new FriendRequest();
        friendRequest4.setSender(user1);
        friendRequest4.setReceiver(user4);
        friendRequest4.setDateAccepted(new Date());
        friendRequestService.save(friendRequest4);

        gameService.addSpectatorsToGame(game);
        assertEquals(1, game.getSpectators().size());
    }

    @Test
    @Transactional
    public void declareWinners() throws PlayerAlreadyInGameException, TooManyPlayersException{
        Game game = new Game();
        gameService.addPlayerToGame(game, user0);
        gameService.addPlayerToGame(game, user1);
        gameService.addPlayerToGame(game, user2);
        game.setStartDate(new Date());
        game.setEndDate(new Date());
        gameService.saveGame(game);
        
        game = gameService.findById(game.getId());

        Player winner = game.getPlayers().getFirst();
        winner.setPoints(2);
        playerService.savePlayer(winner);

        gameService.declareWinners(game);

        game = gameService.findById(game.getId());
        assertEquals(winner.getId(), game.getWinners().getFirst().getId());
    }


}
