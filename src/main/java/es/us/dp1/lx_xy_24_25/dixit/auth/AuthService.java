package es.us.dp1.lx_xy_24_25.dixit.auth;

import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.request.SignupRequest;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerService;
import es.us.dp1.lx_xy_24_25.dixit.user.Authorities;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AuthoritiesService authoritiesService;
	private final UserService userService;
	private final PlayerService playerService;


	@Autowired
	public AuthService(AuthoritiesService authoritiesService, UserService userService, PlayerService playerService, AchievementService achievementService) {
		this.authoritiesService = authoritiesService;
		this.userService = userService;
		this.playerService = playerService;
	}

	@Transactional
	public void createUser(@Valid SignupRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(request.getPassword());
		String strRoles = request.getAuthority();
		Authorities role;

		switch (strRoles.toLowerCase()) {
		case "admin":
			role = authoritiesService.findByAuthority("ADMIN");
			user.setAuthority(role);
			userService.saveUser(user);
			break;
		default:
            role = authoritiesService.findByAuthority("PLAYER");
            user.setAuthority(role);
            Player player = new Player();
            User u = userService.saveUser(user);
            player.setUser(u);
            playerService.savePlayer(player);
		}
	}

}
