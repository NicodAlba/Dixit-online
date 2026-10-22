package es.us.dp1.lx_xy_24_25.dixit.auth;

import es.us.dp1.lx_xy_24_25.dixit.auth.payload.request.SignupRequest;
import es.us.dp1.lx_xy_24_25.dixit.player.Player;
import es.us.dp1.lx_xy_24_25.dixit.player.PlayerService;
import es.us.dp1.lx_xy_24_25.dixit.user.AuthoritiesService;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Users & Admin Module")
@Feature("Authentication")
@Owner("DP1-tutors")
@SpringBootTest
public class AuthServiceTests {

	@Autowired
	protected AuthService authService;
	@Autowired
	protected UserService userService;
	@Autowired
	protected AuthoritiesService authoritiesService;
    @Autowired
    protected PlayerService playerService;

    @Test
	@Transactional
	public void shouldCreateAdminUser() {
		SignupRequest request = createRequest("ADMIN", "admin2");
		int userFirstCount = ((Collection<User>) this.userService.findAll()).size();
		this.authService.createUser(request);
		int userLastCount = ((Collection<User>) this.userService.findAll()).size();
		assertEquals(userFirstCount + 1, userLastCount);
	}



	@Test
	@Transactional
	public void shouldCreatePlayerUser() {
		SignupRequest request = createRequest("PLAYER", "playertest");
		int userFirstCount = ((Collection<User>) this.userService.findAll()).size();
		int playerFirstCount = ((Collection<Player>) this.playerService.findAll()).size();
		this.authService.createUser(request);
		int userLastCount = ((Collection<User>) this.userService.findAll()).size();
		int playerLastCount = ((Collection<Player>) this.playerService.findAll()).size();
		assertEquals(userFirstCount + 1, userLastCount);
		assertEquals(playerFirstCount + 1, playerLastCount);
	}

	private SignupRequest createRequest(String auth, String username) {
		SignupRequest request = new SignupRequest();
		request.setAuthority(auth);
		request.setPassword("prueba");
		request.setUsername(username);

		if(auth == "PLAYER") {
			User playerUser = new User();
			playerUser.setUsername("playerTest");
			playerUser.setPassword("playerTest");
			playerUser.setAuthority(authoritiesService.findByAuthority("PLAYER"));
			userService.saveUser(playerUser);
		}

		return request;
	}

}
