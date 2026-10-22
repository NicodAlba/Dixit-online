package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/turns")
@Tag(name = "Turns")
@SecurityRequirement(name = "bearerAuth")
public class TurnRestController {
    private final TurnService turnService;

    @Autowired
    public TurnRestController(TurnService turnService) {
        this.turnService = turnService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a turn if the user is a player of the turn's game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
        @ApiResponse(responseCode = "404")
    })
    public Turn getTurn(@PathVariable("id") Integer id) {
        return turnService.findById(id);
    }

    @PutMapping("/{turnId}/answer/{cardId}")
    @Operation(summary = "Sets the card chosen by the storyteller as answer of the turn")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<?> setStorytellerAnswer(
        @PathVariable Integer turnId,
        @PathVariable Integer cardId
    ) {
        Turn turn;
        Card card;
        Turn updatedTurn;
        try {
            turn = turnService.findById(turnId);
            card = turnService.findCardById(cardId);
            updatedTurn = turnService.setStorytellerAnswer(turn, card);
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.ok(updatedTurn);
    }

    @PutMapping("/{turnId}/clue")
    @Operation(summary = "Sets the clue chosen by the storyteller")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<?> setStorytellerClue(
        @PathVariable Integer turnId,
        @RequestBody String clue
    ) {
        Turn turn;
        Turn updatedTurn;
        try {
            turn = turnService.findById(turnId);

            updatedTurn = turnService.setStorytellerClue(turn, clue);
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
            } else  {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.ok(updatedTurn);
    }

    @PostMapping("/{turnId}/play-card/{cardId}/{playerId}")
    @Operation(summary = "Sets the given card as the one played by the given player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<?> playerPlayCard(
        @PathVariable Integer turnId,
        @PathVariable Integer cardId,
        @PathVariable Integer playerId
    ) {
        Turn turn;
        Player player;
        Card card;
        Card playedCard;
        try {
            turn = turnService.findById(turnId);
            player = turnService.findPlayerById(playerId);
            card = turnService.findCardById(cardId);

            playedCard = turnService.playerPlayCard(turn, player, card);
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.NOT_FOUND);
            } else   {
                return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.ok(playedCard);
    }

    @PostMapping("/{turnId}/vote/{playerId}/{cardId}")
    @Operation(summary = "Sets the given card as the one voted by the given player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<Card> playerVote(
        @PathVariable Integer turnId,
        @PathVariable Integer playerId,
        @PathVariable Integer cardId
    ) {
        Card updatedCard = turnService.playerVote(turnId, playerId, cardId);

        return ResponseEntity.ok(updatedCard);
    }
}
