package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.InvalidRequestException;
import java.util.Objects;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.GameRepository;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.PlayerAlreadyInGameException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.TooManyPlayersException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameInvitationService {

    private final GameInvitationRepository gameInvitationRepository;

    private final GameService gameService;
    private final GameRepository games;
    private final UserService userService;
    private final FriendRequestService friendRequestService;

    public GameInvitationService(GameInvitationRepository gameInvitationRepository, GameService gameService, UserService userService, FriendRequestService friendRequestService, GameRepository games) {
        this.gameInvitationRepository = gameInvitationRepository;
        this.games = games;
        this.gameService = gameService;
        this.userService = userService;
        this.friendRequestService = friendRequestService;
    }

    @Transactional(rollbackFor = {PlayerAlreadyInGameException.class, TooManyPlayersException.class})
    public Game acceptInvitation(Integer invitationId, User user)
        throws PlayerAlreadyInGameException, TooManyPlayersException {
        GameInvitation invitation = gameInvitationRepository.findByIdForUpdate(invitationId).orElse(null);
        if (invitation == null) return null;
        if (!invitation.getInvited().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not invited to this game");
        }
        Game game = games.findByIdForUpdate(invitation.getGameId()).orElse(null);
        if (game == null || game.getStartDate() != null || game.hasFinished()
            || game.isCancelled() || !game.getTurns().isEmpty()) {
            gameInvitationRepository.delete(invitation);
            // Return normally so expiry commits; the controller translates it to HTTP 410 afterwards.
            return null;
        }
        gameService.addPlayerToGame(game, user);
        gameInvitationRepository.delete(invitation);
        return game;
    }

    @Transactional
    public List<GameInvitation> findAllByUser(User user) {
        gameInvitationRepository.deleteUnavailableByInvited(user);
        return gameInvitationRepository.findAllByInvited(user);
    }

    @Transactional
    public void sendInvitation(String inviter, User invitee, Integer gameId,  String password) {
        User actor = userService.findCurrentUser();
        Game game = gameService.findByIdForUpdate(gameId);
        if (!gameService.playerInGame(game, actor)) throw new AccessDeniedException("Only lobby participants can invite friends.");
        try { gameService.ensureLobbyOpen(game); }
        catch (IllegalStateException ex) { throw new InvalidRequestException(ex.getMessage()); }
        if (friendRequestService.getFriends(actor).stream().noneMatch(friend -> Objects.equals(friend.getId(), invitee.getId()))) {
            throw new AccessDeniedException("You can only invite your friends.");
        }
        if (gameInvitationRepository.findAllByInvited(invitee).stream().anyMatch(i -> Objects.equals(i.getGameId(), gameId))) {
            throw new InvalidRequestException("This friend already has an invitation to the lobby.");
        }
        GameInvitation invitation = new GameInvitation();
        invitation.setInviter(actor.getUsername());
        invitation.setGameId(gameId);
        invitation.setInvited(invitee);
        invitation.setPassword(game.getPassword());
        gameInvitationRepository.save(invitation);
    }

}
