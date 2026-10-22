package es.us.dp1.lx_xy_24_25.dixit.achievement;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.card.CardRepository;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.game.Turn;
import es.us.dp1.lx_xy_24_25.dixit.game.TurnRepository;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerService;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@Epic("Achievements")
class AchievementServiceTest {

    @Autowired
    private AchievementService achievementService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthoritiesService authService;
    @Autowired
    private GameService gameService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private TurnRepository turnRepository;
    @Autowired
    private CardRepository cardRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setup(){
        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setAuthority(authService.findByAuthority("PLAYER"));
        userService.saveUser(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password");
        user2.setAuthority(authService.findByAuthority("PLAYER"));
        userService.saveUser(user2);
    }


    @Test
    @Transactional
    @DisplayName("Should find all the achievements of a user")
    void findAllAchievementsByUserId(){
        Achievement a1 = new Achievement();
        a1.setThreshold(1);
        a1.setName("first");
        a1.setDescription("1");
        a1.setUser(user1);
        achievementService.saveAchievement(a1);
        Achievement a2 = new Achievement();
        a2.setThreshold(1);
        a2.setName("Second");
        a2.setDescription("1");
        a2.setUser(user2);
        achievementService.saveAchievement(a2);

        List<Achievement> achievements = achievementService.findAllAchievementsByUserId(user1.getId());
        assertEquals(1, achievements.size());
    }

    @Test
    @Transactional
    @Feature("Initialize achievements")
    @DisplayName("Should initialize the achievements of type 'WIN' of a user")
    void initializeWinAchievements() {
        List<Achievement> winAchievements = achievementService.initializeWinAchievements(user1);
        assertEquals(4, winAchievements.size());
    }

    @Test
    @Transactional
    @Feature("Initialize achievements")
    @DisplayName("Should initialize the achievements of type 'PLAY' of a user")
    void initializePlayAchievements() {
        List<Achievement> playAchievements = achievementService.initializePlayAchievements(user1);
        assertEquals(4, playAchievements.size());
    }

    @Test
    @Transactional
    @Feature("Initialize achievements")
    @DisplayName("Should initialize the achievements of type 'TRICK' of a user")
    void initializeTrickAchievements() {
        List<Achievement> playAchievements = achievementService.initializeTrickAchievements(user1);
        assertEquals(4, playAchievements.size());
    }

    @Test
    @Transactional
    void checkWinAchievements(){
        Game game = new Game();
        gameService.saveGame(game);

        Player player1 = new Player();
        player1.setUser(user1);
        player1.setGame(game);
        List<Player> players1 = new ArrayList<>();
        players1.add(player1);
        playerService.savePlayer(player1);

        user1 = userService.findById(user1.getId());
        user1.setPlayers(players1);

        Player player2 = new Player();
        player2.setUser(user2);
        player2.setGame(game);
        List<Player> players2 = new ArrayList<>();
        players2.add(player2);
        playerService.savePlayer(player2);

        user1 = userService.findById(user1.getId());
        user1.setPlayers(players1);

        user2 = userService.findById(user2.getId());
        user2.setPlayers(players2);

        game = gameService.findById(game.getId());
        game.setWinners(players1);
        gameService.saveGame(game);

        List<Achievement> winAchievements1 = achievementService.initializeWinAchievements(user1);
        List<Achievement> winAchievements2 = achievementService.initializeWinAchievements(user2);
        user1.setAchievements(winAchievements1);
        userService.saveUser(user1);
        achievementService.checkWinAchievements(user1);
        user2.setAchievements(winAchievements2);
        userService.saveUser(user2);
        achievementService.checkWinAchievements(user2);
        user1 = userService.findById(user1.getId());
        user2 = userService.findById(user2.getId());
        assertNotNull(user1.getAchievements().getFirst().getDateObtained());
        assertNull(user2.getAchievements().getFirst().getDateObtained());
    }

    @Test
    @Transactional
    void checkPlayAchievements(){
        Game game = new Game();
        gameService.saveGame(game);

        Player player1 = new Player();
        player1.setUser(user1);
        player1.setGame(game);
        List<Player> players = new ArrayList<>();
        players.add(player1);
        playerService.savePlayer(player1);

        user1 = userService.findById(user1.getId());
        user1.setPlayers(players);

        gameService.saveGame(game);

        Player player2 = new Player();
        player2.setUser(user2);
        player2.setGame(game);
        List<Player> players2 = new ArrayList<>();
        players2.add(player2);
        playerService.savePlayer(player2);

        user2 = userService.findById(user2.getId());
        user2.setPlayers(players2);

        List<Achievement> playAchievements1 = achievementService.initializePlayAchievements(user1);
        List<Achievement> playAchievements2 = achievementService.initializeWinAchievements(user2);
        user1.setAchievements(playAchievements1);
        userService.saveUser(user1);
        achievementService.checkPlayAchievements(user1);
        assertNull(playAchievements1.getFirst().getDateObtained());

        User user3 = new User();
        user3.setUsername("play-third");
        user3.setPassword("password");
        user3.setAuthority(authService.findByAuthority("PLAYER"));
        userService.saveUser(user3);
        Player player3 = new Player();
        player3.setUser(user3);
        player3.setGame(game);
        playerService.savePlayer(player3);
        game.getPlayers().addAll(List.of(player1, player2, player3));
        game.setStartDate(new java.util.Date());
        game.setEndDate(new java.util.Date());
        gameService.saveGame(game);
        achievementService.checkPlayAchievements(user1);
        user2.setAchievements(playAchievements2);
        userService.saveUser(user2);
        achievementService.checkWinAchievements(user2);
        user1 = userService.findById(user1.getId());
        user2 = userService.findById(user2.getId());
        assertNotNull(user1.getAchievements().getFirst().getDateObtained());
        assertNull(user2.getAchievements().getFirst().getDateObtained());
    }

    @Test
    @Transactional
    void checkTrickAchievements(){
        Game game = new Game();
        gameService.saveGame(game);
        List<Turn> turns = new ArrayList<>();
        Turn turn = new Turn();
        turn.setGame(game);
        turns.add(turn);
        game.setTurns(turns);
        Card card = new Card();
        card.setGame(game);
        card.setTurn(turn);
        List<Card> cards = new ArrayList<>();
        cards.add(card);
        turn.setPlayedCards(cards);

        Player player1 = new Player();
        player1.setUser(user1);
        player1.setGame(game);
        List<Player> players1 = new ArrayList<>();
        players1.add(player1);
        player1.setCards(cards);
        playerService.savePlayer(player1);
        card.setOwner(player1);
        card.setUser(user1);

        user1 = userService.findById(user1.getId());
        user1.setPlayers(players1);

        Player player2 = new Player();
        player2.setUser(user2);
        player2.setGame(game);
        List<Player> votes = new ArrayList<>();
        votes.add(player2);
        playerService.savePlayer(player2);
        card.setVotes(votes);

        User user3 = new User();
        user3.setUsername("user3");
        user3.setPassword("password");
        user3.setAuthority(authService.findByAuthority("PLAYER"));
        userService.saveUser(user3);
        Player storyteller = new Player();
        storyteller.setUser(user3);
        storyteller.setGame(game);
        turn.setStoryteller(storyteller);
        playerService.savePlayer(storyteller);

        cardRepository.save(card);
        turnRepository.save(turn);
        gameService.saveGame(game);

        TrickAchievement tA = new TrickAchievement();
        tA.setUser(user1);
        tA.setName("test");
        tA.setThreshold(1);
        tA.setDescription("test");
        Achievement t = achievementService.saveAchievement(tA);
        user1.setAchievements(List.of(t));
        achievementService.checkTrickAchievements(user1);
        user1 = userService.findById(user1.getId());
        assertNotNull(user1.getAchievements().getFirst().getDateObtained());
    }

}
