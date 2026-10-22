package es.us.dp1.lx_xy_24_25.dixit.message;

import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Epic("Social")
@Feature("Messages")
class MessageServiceTest {

    @Autowired
    private AuthoritiesService authoritiesService;
    @Autowired
    private UserService userService;
    @Autowired
    private GameService gameService;
    @Autowired
    private MessageService messageService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setUsername("user");
        user.setPassword("password");
        user.setAuthority(authoritiesService.findByAuthority("PLAYER"));
        userService.saveUser(user);
    }

    @Test
    @Transactional
    void findMessagesByPlayerId() {
        Game game = gameService.createGame("", user);

        Player player = game.getPlayers().getFirst();

        int n = 4;

        for (int i = 0; i < n; i++) {
            Message message = new Message();
            message.setText("test message " + i);
            message.setPlayer(player);
            message.setGame(game);
            message.setDate(new Date());
            messageService.saveMessage(message);
        }
        List<Message> messages = messageService.findMessagesByPlayerId(player.getId());
        assertEquals(player.getId(), messages.getFirst().getPlayer().getId());
        assertEquals(n, messageService.findMessagesByPlayerId(player.getId()).size());
    }

    @Test
    @Transactional
    void findMessagesByGameId() {
        Game game = gameService.createGame("", user);

        Player player = game.getPlayers().getFirst();

        int n = 4;
        for (int i = 0; i < n; i++) {
            Message message = new Message();
            message.setText("test message " + i);
            message.setPlayer(player);
            message.setGame(game);
            message.setDate(new Date());
            messageService.saveMessage(message);
        }
        assertEquals(n, messageService.findMessagesByGameId(game.getId()).size());
    }
}
