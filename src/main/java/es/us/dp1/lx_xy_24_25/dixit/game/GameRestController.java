package es.us.dp1.lx_xy_24_25.dixit.game;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.PlayerAlreadyInGameException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.TooManyPlayersException;
import es.us.dp1.lx_xy_24_25.dixit.social.GameInvitationService;
import es.us.dp1.lx_xy_24_25.dixit.social.InvitationDetails;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import es.us.dp1.lx_xy_24_25.dixit.util.RestPreconditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Games")
@SecurityRequirement(name = "bearerAuth")
public class GameRestController {
    private final GameService gameService;
    private final UserService userService;
    private final TurnService turnService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameInvitationService gameInvitationService;


    @Autowired
    public GameRestController(GameService gameService, UserService userService, TurnService turnService, SimpMessagingTemplate messagingTemplate, GameInvitationService gameInvitationService){
        this.gameService = gameService;
        this.userService = userService;
        this.turnService = turnService;
        this.messagingTemplate = messagingTemplate;
        this.gameInvitationService = gameInvitationService;
    }

    @GetMapping
    @Operation(summary = "Retrieves games", description = "Retrieves all games")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<Iterable<Game>> findAll() {
        Iterable<Game> res = gameService.findAll();
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(value = "{id}")
    @Operation(summary = "Gets a game if the user is a player in that game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403", description = "User can't see the game since they're not a player of it"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        User currentUser = userService.findCurrentUser();
        Game game;
        try {
            game = gameService.findByIdIfUserInGame(id, currentUser);
        } catch (Exception e) {
            switch (e) {
                case DataAccessException dataAccessException -> {
                    return new ResponseEntity<>(new MessageResponse(dataAccessException.getMessage()), HttpStatus.FORBIDDEN);
                }
                case ResourceNotFoundException resourceNotFoundException -> {
                    return new ResponseEntity<>(new MessageResponse(resourceNotFoundException.getMessage()), HttpStatus.NOT_FOUND);
                }
                default -> {
                    return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        return new ResponseEntity<>(game, HttpStatus.OK);
    }

    @GetMapping("/inProgress")
    @Operation(summary = "Retrieves games in progress", description = "Retrieves all the games that have a start date and end date is null")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<List<Game>> getGamesInProgress() {
        List<Game> gamesInProgress = gameService.findPlaying();

        if(gamesInProgress.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(gamesInProgress, HttpStatus.OK);
    }

    @GetMapping("/pastGames")
    @Operation(summary = "Retrieves finished games")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<List<Game>> getGamesPastGames() {
        List<Game> gamesInProgress = gameService.findPast();
        return new ResponseEntity<>(gamesInProgress, HttpStatus.OK);
    }


    @GetMapping("/my")
    @Operation(summary = "Gets the games where the given user has participated", description = "Retrieves all the games where the user with the given username has participated if the request comes from said user or from an ADMIN user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200")
    })
    public ResponseEntity<List<Game>> findMyGames() {
        User currentUser = userService.findCurrentUser();
        List<Game> myGames = gameService.findGamesByPlayerUsername(currentUser.getUsername());

        return new ResponseEntity<>(myGames, HttpStatus.OK);
    }

    @GetMapping("/games/{username}")
    @Operation(summary = "Gets the games where the given user has participated", description = "Retrieves all the games where the user with the given username has participated if the request comes from said user or from an ADMIN user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403", description = "User can't see the games of the given user because they're not an ADMIN")
    })
    public ResponseEntity<?> findGamesByPlayerUsername(@PathVariable String username) {
        List<Game> games = gameService.findGamesByPlayerUsername(username);

        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Creates a new game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204")
    })
    public ResponseEntity<Game> create(@RequestBody HashMap<String,String> body) {
        String password = body.get("password");
        User currentUser = userService.findCurrentUser();
        Game game = gameService.createGame(password, currentUser);
        return new ResponseEntity<>(game, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/join")
    @Operation(summary = "Allows a user to join a game", description = "Allows to join the lobby of a game if the id and the password are correct, there is room for them and they're not already in that game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409", description = "Number of players already reached its limit"),
        @ApiResponse(responseCode = "409", description = "Player is already in the game")
    })
    public ResponseEntity<?> joinGame(@PathVariable Integer id, @RequestBody HashMap<String, String> body) throws TooManyPlayersException, PlayerAlreadyInGameException {
        User currentUser = userService.findCurrentUser();
        Game game;
        try {
            game = gameService.joinGame(id, currentUser, body.get("password"));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.UNAUTHORIZED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.CONFLICT);
        }

        messagingTemplate.convertAndSend("/topic/game/" + id, game);

        return new ResponseEntity<>(game, HttpStatus.OK);
    }

    @PutMapping(value = "{id}")
    @Operation(summary = "Updates a game by the given id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<Game> update(@PathVariable Integer id, @RequestBody @Valid Game game) {
        RestPreconditions.checkNotNull(gameService.findById(id), "Game", "ID", id);
        Game updatedGame = gameService.updateGame(game, id);
        return new ResponseEntity<>(updatedGame, HttpStatus.OK);
    }

    @PatchMapping("/{id}/leave")
    @Operation(summary = "Allows players to leave the lobby of a game", description = "Allows a player to leave the lobby of a game. In the case the owner of the game leaves, the game is deleted")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<?> leaveGame(@PathVariable Integer id) {
        Game game = gameService.findById(id);
        User currentUser = userService.findCurrentUser();

        boolean isLeader = game.getLeader().equals(currentUser);

        if (isLeader) {
            gameService.deleteGame(game);
            messagingTemplate.convertAndSend("/topic/game/" + id, "DELETED");
            return new ResponseEntity<>(new MessageResponse("Game deleted successfully."), HttpStatus.OK);
        } else {
            gameService.removePlayerFromGame(game, currentUser);

            messagingTemplate.convertAndSend("/topic/game/" + id, game);
            return new ResponseEntity<>(new MessageResponse("Left game successfully."), HttpStatus.OK);
        }
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Starts a game", description = "Allows the leader of a game to start the game if the minimum number of players is reached")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403", description = "User can't start the game because they're not the leader"),
        @ApiResponse(responseCode = "404")

    })
    public ResponseEntity<?> startGame(@PathVariable Integer id) {
        Game game;
        try {
            game = turnService.startGame(id, userService.findCurrentUser());
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
        messagingTemplate.convertAndSend("/topic/game/" + id, game);

        return new ResponseEntity<>(game, HttpStatus.OK);
    }

    @DeleteMapping(value = "{id}")
    @Operation(summary = "Deletes a game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "403", description = "User can't delete the game because they're not the leader nor an ADMIN"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer id) {
        RestPreconditions.checkNotNull(gameService.findById(id), "Game", "ID", id);
        if (!userService.findCurrentUser().hasAuthority("ADMIN")) {
            return new ResponseEntity<>(new MessageResponse("Only admins can delete"), HttpStatus.FORBIDDEN);
        }
        Game game =  gameService.findById(id);
        gameService.deleteGame(game);
        return new ResponseEntity<>(new MessageResponse("Game deleted!"), HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "{gameId}/invite")
    @Operation(summary = "Sends an invitation to the player in the body of the request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<MessageResponse> sendInvitation(@PathVariable Integer gameId, @RequestBody @Valid InvitationDetails invitation) {
        RestPreconditions.checkNotNull(gameService.findById(gameId), "Game", "ID", gameId);
        User invitedUser = userService.findById(Integer.valueOf(invitation.getInvited()));
        User currentUser = userService.findCurrentUser();
        gameInvitationService.sendInvitation(currentUser.getUsername(), invitedUser, gameId, invitation.getPassword());
        return new ResponseEntity<>(new MessageResponse("Invitation sent!"), HttpStatus.OK);
    }

    @GetMapping(value = "/invitations")
    @Operation(summary = "Retrieves all the invitations that the current user received")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400")
    })
    public ResponseEntity<?> findAllByUser() {
        User user = userService.findCurrentUser();
        return new ResponseEntity<>(gameInvitationService.findAllByUser(user), HttpStatus.OK);
    }

    @PatchMapping(value = "/invitations/{invitationId}/accept")
    @Operation(summary = "Allows the user to accept the given invitation and join the game", description = "Allows to join the lobby of a game through the invitation if the id and the password are correct, there is room for them and they're not already in that game")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409", description = "Number of players already reached its limit"),
        @ApiResponse(responseCode = "409", description = "Player is already in the game")
    })
    public ResponseEntity<MessageResponse> acceptInvitation(@PathVariable Integer invitationId) throws TooManyPlayersException, PlayerAlreadyInGameException {
        Game game;
        try {
            game = gameInvitationService.acceptInvitation(invitationId, userService.findCurrentUser());
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.CONFLICT);
        }
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), game);
        return new ResponseEntity<>(new MessageResponse("Invitation successfully accepted!"), HttpStatus.OK);
    }

    @PatchMapping("/{id}/spectate/{playerId}")
    @Operation(summary = "Allows a user to spectate a game", description = "Allows to join the game as an spectator if teh user is friend of all the players in the game.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404"),
        @ApiResponse(responseCode = "409", description = "You are not friend of all players"),
    })
    public ResponseEntity<?> spectateGame(@PathVariable Integer id, @PathVariable Integer playerId) {
        Game game = gameService.findById(id);
        User currentUser = userService.findCurrentUser();

        if (!game.getSpectators().contains(currentUser)) {
            return new ResponseEntity<>(new MessageResponse("You are not a friend of all players in the game"), HttpStatus.UNAUTHORIZED);
        }

        messagingTemplate.convertAndSend("/topic/game/" + id, game);

        return new ResponseEntity<>(game, HttpStatus.OK);
    }

}
