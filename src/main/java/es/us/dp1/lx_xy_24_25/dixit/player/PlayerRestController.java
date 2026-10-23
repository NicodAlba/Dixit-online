package es.us.dp1.lx_xy_24_25.dixit.player;

import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Player")
@SecurityRequirement(name = "bearerAuth")
public class PlayerRestController {

    private final PlayerService playerService;
    private final UserService userService;

    @Autowired
    public PlayerRestController(PlayerService playerService, UserService userService) {
        this.playerService = playerService;
        this.userService = userService;
    }


    @GetMapping("{id}")
    @Operation(summary = "Gets a player by its id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<Player> getPlayer(@PathVariable("id") Integer id) {
        try {
            Player player = playerService.findById(id);
            User actor = userService.findCurrentUser();
            // Until actor projections (U5), only an owner's hand can be returned.
            if (!player.getUser().getId().equals(actor.getId())) throw new AccessDeniedException("Only the participant can read their hand.");
            return new ResponseEntity<>(player, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/cards")
    @Operation(summary = "Retrieves the cards of the given player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403")
    })
    public ResponseEntity<List<Card>> getPlayerCards(@PathVariable("id") Integer id) {
        User currentUser = userService.findCurrentUser();
        Player player;
        try {
            player = playerService.findById(id);
            if (currentUser.getPlayers().contains(player)) {
                return new ResponseEntity<>(player.getCards(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
