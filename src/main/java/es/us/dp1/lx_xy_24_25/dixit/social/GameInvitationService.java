package es.us.dp1.lx_xy_24_25.dixit.social;

import es.us.dp1.lx_xy_24_25.dixit.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameInvitationService {

    private final GameInvitationRepository gameInvitationRepository;

    public GameInvitationService(GameInvitationRepository gameInvitationRepository) {
        this.gameInvitationRepository = gameInvitationRepository;
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
        if (!password.isBlank()) {
            invitation.setPassword(password);
        }
        gameInvitationRepository.save(invitation);
    }

}
