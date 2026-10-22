package es.us.dp1.lx_xy_24_25.dixit.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.social.InvitationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.dp1.lx_xy_24_25.dixit.configuration.SecurityConfiguration;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.social.GameInvitation;
import es.us.dp1.lx_xy_24_25.dixit.social.GameInvitationRepository;
import es.us.dp1.lx_xy_24_25.dixit.social.GameInvitationService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;

@WebMvcTest(controllers = GameRestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class), excludeAutoConfiguration = SecurityConfiguration.class)
public class GameRestControllerTest {

    private static final String BASE_URL = "/api/v1/games";
    private static final Integer GAME_ID = 1;
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserService userService;

    @MockBean
    private TurnService turnService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private GameInvitationService gameInvitationService;

    @MockBean
    private GameInvitationRepository gameInvitationRepository;

    private Game testGame;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setId(1);

        User user2 = new User();
        user2.setId(2);
        user2.setUsername("user2");

        User user3 = new User();
        user3.setId(3);
        user3.setUsername("user3");

        Player p1 = new Player();
        p1.setUser(testUser);

        Player p2 = new Player();
        p2.setUser(user2);
        p2.setId(2);

        Player p3 = new Player();
        p3.setUser(user3);

        testGame = new Game();
        testGame.setId(GAME_ID);
        testGame.setPassword(PASSWORD);
        testGame.setLeader(testUser);

        testGame.getPlayers().add(p1);
        testGame.getPlayers().add(p2);
        testGame.getPlayers().add(p3);
    }




    @Test
    @WithMockUser
    void testFindAllSuccess() throws Exception {
        when(gameService.findAll()).thenReturn(List.of(testGame));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(GAME_ID));
    }

    @Test
    @WithMockUser
    void testFindByIdSuccess() throws Exception {
        when(userService.findCurrentUser()).thenReturn(testUser);
        when(gameService.findByIdIfUserInGame(GAME_ID, testUser)).thenReturn(testGame);

        mockMvc.perform(get(BASE_URL + "/{id}", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GAME_ID));
    }

    @Test
    @WithMockUser
    void testFindByIdNotFound() throws Exception {
        when(userService.findCurrentUser()).thenReturn(testUser);
        when(gameService.findByIdIfUserInGame(anyInt(), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Game not found"));

        mockMvc.perform(get(BASE_URL + "/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testCreateGameSuccess() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("password", PASSWORD);

        when(userService.findCurrentUser()).thenReturn(testUser);
        when(gameService.createGame(PASSWORD, testUser)).thenReturn(testGame);

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(GAME_ID));
    }

    @Test
    @WithMockUser
    void testJoinGameSuccess() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("password", PASSWORD);

        when(gameService.findById(GAME_ID)).thenReturn(testGame);
        when(userService.findCurrentUser()).thenReturn(testUser);

        mockMvc.perform(patch(BASE_URL + "/{id}/join", GAME_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(gameService).addPlayerToGame(testGame, testUser);
    }

    @Test
    @WithMockUser
    void testJoinGameWrongPassword() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("password", "wrong_pass");

        when(gameService.findById(GAME_ID)).thenReturn(testGame);

        mockMvc.perform(patch(BASE_URL + "/{id}/join", GAME_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect password"));
    }

    @Test
    @WithMockUser
    void testLeaveGameAsLeader() throws Exception {
        when(gameService.findById(GAME_ID)).thenReturn(testGame);
        when(userService.findCurrentUser()).thenReturn(testUser);

        mockMvc.perform(patch(BASE_URL + "/{id}/leave", GAME_ID)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Game deleted successfully."));

        verify(gameService).deleteGame(testGame);
    }

    @Test @WithMockUser void testStartGameSuccess() throws Exception {
        when(gameService.findById(GAME_ID)).thenReturn(testGame);
        mockMvc.perform(patch(BASE_URL + "/{id}/start", GAME_ID)
            .with(csrf())).andExpect(status().isOk());
        verify(gameService).saveGame(any(Game.class));
        verify(turnService).initializeFirstTurn(any(Game.class)); }


    @Test
    @WithMockUser
    void testAcceptInvitationSuccess() throws Exception {
        Integer invId = 100;
        GameInvitation invitation = new GameInvitation();
        invitation.setId(invId);
        invitation.setGameId(GAME_ID);
        invitation.setInvited(testUser);

        when(gameInvitationRepository.findById(invId)).thenReturn(Optional.of(invitation));
        when(userService.findCurrentUser()).thenReturn(testUser);
        when(gameService.findById(GAME_ID)).thenReturn(testGame);

        mockMvc.perform(patch(BASE_URL + "/invitations/{invitationId}/accept", invId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation successfully accepted!"));

        verify(gameService).addPlayerToGame(testGame, testUser);
    }

    @Test
    @WithMockUser
    void testSendInvitation() throws Exception {
        when(userService.findCurrentUser()).thenReturn(testUser);
        when(gameService.findById(GAME_ID)).thenReturn(testGame);

        InvitationDetails invitation = new InvitationDetails();
        invitation.setInvited(testUser.getId().toString());
        invitation.setPassword(testGame.getPassword());

        mockMvc.perform(post(BASE_URL + "/{gameId}/invite", GAME_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invitation)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Invitation sent!"));
    }

    @Test
    @WithMockUser
    void testCanAddSpectatorToGame() throws Exception {
        when(gameService.findById(GAME_ID)).thenReturn(testGame);
        when(userService.findCurrentUser()).thenReturn(testUser);
        testGame.setSpectators(List.of(testUser));
        Integer somePlayerId = testGame.getPlayers().get(1).getId();

        mockMvc.perform(patch(BASE_URL + "/{id}/spectate/{playerId}", GAME_ID, somePlayerId)
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testCannotAddSpectatorToGame() throws Exception {
        when(gameService.findById(GAME_ID)).thenReturn(testGame);
        when(userService.findCurrentUser()).thenReturn(testUser);
        testGame.setSpectators(List.of());
        Integer somePlayerId = testGame.getPlayers().get(1).getId();

        mockMvc.perform(patch(BASE_URL + "/{id}/spectate/{playerId}", GAME_ID, somePlayerId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
