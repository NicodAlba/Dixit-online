package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.card.Card;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
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
        return turnService.findVisibleById(id);
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
        Turn updatedTurn;
        try {
            updatedTurn = turnService.selectAnswer(turnId, cardId);
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) throw (AccessDeniedException)e;
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
        Turn updatedTurn;
        try {
            updatedTurn = turnService.provideClue(turnId, clue);
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) throw (AccessDeniedException)e;
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
        Card playedCard;
        try {
            playedCard = turnService.playCard(turnId, playerId, cardId);
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) throw (AccessDeniedException)e;
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
