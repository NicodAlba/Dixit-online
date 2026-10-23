package es.us.dp1.lx_xy_24_25.dixit.message;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import es.us.dp1.lx_xy_24_25.dixit.util.RestPreconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages")
@SecurityRequirement(name = "bearerAuth")
public class MessageRestController {

    private final MessageService messageService;
    private final GameService gameService;
    private final UserService userService;

    @Autowired
    public MessageRestController(MessageService messageService, GameService gameService, UserService userService) {
        this.messageService = messageService;
        this.gameService = gameService;
        this.userService = userService;
    }

    @GetMapping("/games/{gameId}")
    @Operation(summary = "Get all messages for a specific game")
    public ResponseEntity<?> getMessagesByGameId(@PathVariable Integer gameId) {
        try {
            // Verify game exists
            RestPreconditions.checkNotNull(gameService.findById(gameId), "Game", "ID", gameId);
            
            // Verify user is in the game (optional security check, good practice)
            User currentUser = userService.findCurrentUser();
            gameService.findByIdIfUserInGame(gameId, currentUser);
             
            List<Message> messages = messageService.findMessagesByGameId(gameId);
            List<MessageDTO> messageDTOs = messages.stream().map(MessageDTO::new).toList();
            return new ResponseEntity<>(messageDTOs, HttpStatus.OK);
        } catch (AccessDeniedException ex) { throw ex;
        } catch (Exception e) {
             return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/games/{gameId}")
    @Operation(summary = "Create a new message in a game")
    public ResponseEntity<?> createMessage(@PathVariable Integer gameId, @RequestBody @Valid Message message) {
        try {
            Game game = gameService.findById(gameId);
            RestPreconditions.checkNotNull(game, "Game", "ID", gameId);
            
            User currentUser = userService.findCurrentUser();
            Player player = game.getPlayers().stream()
                .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

            if (player == null) {
                return new ResponseEntity<>(new MessageResponse("You are not a player in this game"), HttpStatus.FORBIDDEN);
            }

            message.setGame(game);
            message.setPlayer(player);
            message.setDate(new Date());
            
            Message savedMessage = messageService.saveMessage(message);
            return new ResponseEntity<>(new MessageDTO(savedMessage), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
