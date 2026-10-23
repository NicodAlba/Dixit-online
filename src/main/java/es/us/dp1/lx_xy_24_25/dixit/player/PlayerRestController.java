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

    private final es.us.dp1.lx_xy_24_25.dixit.game.GameViews views;

    public PlayerRestController(es.us.dp1.lx_xy_24_25.dixit.game.GameViews views) { this.views=views; }

    @GetMapping("/{id}")
    public ResponseEntity<es.us.dp1.lx_xy_24_25.dixit.game.GameView.Participant> getPlayer(
        @PathVariable Integer id,@org.springframework.web.bind.annotation.RequestParam(required=false) Integer perspectivePlayerId) {
        return ResponseEntity.ok(views.player(id,perspectivePlayerId));
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<es.us.dp1.lx_xy_24_25.dixit.game.GameView.PlayedCard>> getPlayerCards(
        @PathVariable Integer id,@org.springframework.web.bind.annotation.RequestParam(required=false) Integer perspectivePlayerId) {
        return ResponseEntity.ok(views.cards(id,perspectivePlayerId));
    }
}
