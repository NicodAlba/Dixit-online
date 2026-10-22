package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.game.Game;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.PlayerAlreadyInGameException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.TooManyPlayersException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameInvitationService {

    private final GameInvitationRepository gameInvitationRepository;

    private final GameService gameService;

    public GameInvitationService(GameInvitationRepository gameInvitationRepository, GameService gameService) {
        this.gameInvitationRepository = gameInvitationRepository;
        this.gameService = gameService;
    }

    @Transactional(rollbackFor = {PlayerAlreadyInGameException.class, TooManyPlayersException.class})
    public Game acceptInvitation(Integer invitationId, User user)
        throws PlayerAlreadyInGameException, TooManyPlayersException {
        GameInvitation invitation = gameInvitationRepository.findByIdForUpdate(invitationId)
            .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.getInvited().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not invited to this game");
        }
        Game game = gameService.findByIdForUpdate(invitation.getGameId());
        gameService.addPlayerToGame(game, user);
        gameInvitationRepository.delete(invitation);
        return game;
    }

    @Transactional(readOnly = true)
    public List<GameInvitation> findAllByUser(User user) {
        return gameInvitationRepository.findAllByInvited(user);
    }

    @Transactional
    public void sendInvitation(String inviter, User invitee, Integer gameId,  String password) {
        GameInvitation invitation = new GameInvitation();
        invitation.setInviter(inviter);
        invitation.setGameId(gameId);
        invitation.setInvited(invitee);
        if (password != null && !password.isBlank()) {
            invitation.setPassword(password);
        }
        gameInvitationRepository.save(invitation);
    }

}
