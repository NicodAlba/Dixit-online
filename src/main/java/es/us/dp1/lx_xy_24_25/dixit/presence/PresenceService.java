package es.us.dp1.lx_xy_24_25.dixit.presence;

import es.us.dp1.lx_xy_24_25.dixit.configuration.StompAccess;
import es.us.dp1.lx_xy_24_25.dixit.game.GameService;
import es.us.dp1.lx_xy_24_25.dixit.social.FriendRequestService;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PresenceService {
    public record Connection(Integer accountId, boolean online) {}
    private final UserService users;
    private final FriendRequestService friends;
    private final GameService games;
    private final StompAccess sockets;
    public PresenceService(UserService users, FriendRequestService friends, GameService games, StompAccess sockets) {
        this.users=users;this.friends=friends;this.games=games;this.sockets=sockets;
    }
    @Transactional(readOnly=true)
    public List<Connection> visibleConnections(Integer gameId) {
        var actor=users.findCurrentUser();
        List<Integer> visible = gameId == null ? friends.getFriends(actor).stream().map(friend->friend.getId()).toList()
            : games.findByIdIfUserInGame(gameId,actor).getPlayers().stream().map(player->player.getUser().getId()).toList();
        Set<Integer> online=sockets.onlineAccountIds();
        return visible.stream().distinct().sorted().map(id->new Connection(id,online.contains(id))).toList();
    }
}
