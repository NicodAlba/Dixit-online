package es.us.dp1.lx_xy_24_25.dixit.game;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.configuration.SecurityConfiguration;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;

@WebMvcTest(controllers = TurnRestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class), excludeAutoConfiguration = SecurityConfiguration.class)
public class TurnRestControllerTest {

    private static final String BASE_URL = "/api/v1/turns";
    private static final Integer turnId = 1;
    private static final Integer cardId = 10;
    private static final Integer playerId = 5;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TurnService turnService;
    @MockBean private GameViews views;

    @Autowired
    private ObjectMapper objectMapper;

    private Turn testTurn;
    private Card testCard;
    private Player testPlayer;

    @BeforeEach
    void setUp() {
        testTurn = new Turn();
        testTurn.setId(turnId);

        testCard = new Card();
        testCard.setId(cardId);

        testPlayer = new Player();
        testPlayer.setId(playerId);
    }

    @Test
    @WithMockUser("test")
    void testGetTurnSuccess() throws Exception {
        when(views.turn(turnId,null)).thenReturn(new GameView.Round(turnId,TurnState.WAITING_STORYTELLER_CARD,null,null,null,java.util.List.of(),0,false,false,null));

        mockMvc.perform(get(BASE_URL + "/{id}", turnId)).andExpect(status().isOk());
        verify(views).turn(turnId,null);
    }

    @Test
    @WithMockUser("test")
    void testGetTurnNotFound() throws Exception {
        when(views.turn(999,null)).thenThrow(new ResourceNotFoundException("Turn not found"));

        mockMvc.perform(get(BASE_URL + "/{id}", 999)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerAnswerSuccess() throws Exception {
        when(turnService.selectAnswer(turnId, cardId)).thenReturn(testTurn);

        mockMvc.perform(put(BASE_URL + "/{turnId}/answer/{cardId}", turnId, cardId).with(csrf())).andExpect(status().isOk());
        verify(turnService).selectAnswer(turnId, cardId);
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerAnswerTurnNotFound() throws Exception {
        when(turnService.selectAnswer(999, cardId)).thenThrow(new ResourceNotFoundException("Resource not found"));


        mockMvc.perform(put(BASE_URL + "/{turnId}/answer/{cardId}", 999, cardId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerAnswerCardNotFound() throws Exception {
        when(turnService.selectAnswer(turnId, 999)).thenThrow(new ResourceNotFoundException("Resource not found"));


        mockMvc.perform(put(BASE_URL + "/{turnId}/answer/{cardId}", turnId, 999).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerAnswerBadRequest() throws Exception {
        when(turnService.selectAnswer(turnId, cardId)).thenThrow(new IllegalStateException("Invalid state"));

        mockMvc.perform(put(BASE_URL + "/{turnId}/answer/{cardId}", turnId, cardId).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerClueSuccess() throws Exception {
        String clue = "test clue";
        when(turnService.provideClue(eq(turnId), anyString())).thenReturn(testTurn);

        mockMvc.perform(put(BASE_URL + "/{turnId}/clue", turnId).with(csrf()).contentType("application/json").content(objectMapper.writeValueAsString(clue))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser("test")
    void testSetStorytellerClueTurnNotFound() throws Exception {
        when(turnService.provideClue(eq(999), anyString())).thenThrow(new ResourceNotFoundException("Resource not found"));

        String clue = "test clue";

        mockMvc.perform(put(BASE_URL + "/{turnId}/clue", 999).with(csrf()).contentType("application/json").content(objectMapper.writeValueAsString(clue))).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testPlayerPlayCardSuccess() throws Exception {
        when(turnService.playCard(turnId, playerId, cardId)).thenReturn(testCard);

        mockMvc.perform(post(BASE_URL + "/{turnId}/play-card/{cardId}/{playerId}", turnId, cardId, playerId).with(csrf())).andExpect(status().isOk());
        verify(turnService).playCard(turnId, playerId, cardId);
    }

    @Test
    @WithMockUser("test")
    void testPlayerPlayCardTurnNotFound() throws Exception {
        when(turnService.playCard(999, playerId, cardId)).thenThrow(new ResourceNotFoundException("Resource not found"));


        mockMvc.perform(post(BASE_URL + "/{turnId}/play-card/{cardId}/{playerId}", 999, cardId, playerId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testPlayerPlayCardRejectsForgedActor() throws Exception {
        when(turnService.playCard(turnId, 999, cardId)).thenThrow(new AccessDeniedException("Cannot act as another participant"));


        mockMvc.perform(post(BASE_URL + "/{turnId}/play-card/{cardId}/{playerId}", turnId, cardId, 999).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser("test")
    void testPlayerPlayCardCardNotFound() throws Exception {
        when(turnService.playCard(turnId, playerId, 999)).thenThrow(new ResourceNotFoundException("Resource not found"));


        mockMvc.perform(post(BASE_URL + "/{turnId}/play-card/{cardId}/{playerId}", turnId, 999, playerId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testPlayerPlayCardBadRequest() throws Exception {
        when(turnService.playCard(turnId, playerId, cardId)).thenThrow(new IllegalStateException("Invalid operation"));

        mockMvc.perform(post(BASE_URL + "/{turnId}/play-card/{cardId}/{playerId}", turnId, cardId, playerId).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("test")
    void testPlayerVoteSuccess() throws Exception {
        when(turnService.playerVote(turnId, playerId, cardId)).thenReturn(testCard);

        mockMvc.perform(post(BASE_URL + "/{turnId}/vote/{playerId}/{cardId}", turnId, playerId, cardId).with(csrf())).andExpect(status().isOk());
        verify(turnService).playerVote(turnId, playerId, cardId);
    }

    @Test
    @WithMockUser("test")
    void testPlayerVoteTurnNotFound() throws Exception {
        when(turnService.playerVote(999, playerId, cardId)).thenThrow(new ResourceNotFoundException("Turn not found"));

        mockMvc.perform(post(BASE_URL + "/{turnId}/vote/{playerId}/{cardId}", 999, playerId, cardId).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("test")
    void testPlayerVoteRejectsForgedActor() throws Exception {
        when(turnService.playerVote(turnId, 999, cardId)).thenThrow(new AccessDeniedException("Cannot act as another participant"));

        mockMvc.perform(post(BASE_URL + "/{turnId}/vote/{playerId}/{cardId}", turnId, 999, cardId).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser("test")
    void testPlayerVoteCardNotFound() throws Exception {
        when(turnService.playerVote(turnId, playerId, 999)).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(post(BASE_URL + "/{turnId}/vote/{playerId}/{cardId}", turnId, playerId, 999).with(csrf())).andExpect(status().isNotFound());
    }
}