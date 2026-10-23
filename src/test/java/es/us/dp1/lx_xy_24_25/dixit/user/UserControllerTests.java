package es.us.dp1.lx_xy_24_25.dixit.user;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.configuration.SecurityConfiguration;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.AccessDeniedException;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Users & Admin Module")
@Feature("Users Management")
@Owner("DP1-tutors")
@WebMvcTest(controllers = UserRestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSecurityConfigurer.class), excludeAutoConfiguration = SecurityConfiguration.class)
class UserControllerTests {

	private static final int TEST_USER_ID = 1;
	private static final int TEST_AUTH_ID = 1;
	private static final String BASE_URL = "/api/v1/users";

	@SuppressWarnings("unused")
	@Autowired
	private UserRestController userController;

	@MockBean
	private UserService userService;

	@MockBean
	private AuthoritiesService authService;

	@MockBean
	private AchievementService achievementService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mockMvc;

	private Authorities auth;
	private User user, logged;

	@BeforeEach
	void setup() {
		auth = new Authorities();
		auth.setId(TEST_AUTH_ID);
		auth.setAuthority("PLAYER");

		user = new User();
		user.setId(1);
		user.setUsername("user");
		user.setPassword("password");
		user.setAuthority(auth);

		when(this.userService.findCurrentUser()).thenReturn(getUserFromDetails(
				(UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
	}

	private User getUserFromDetails(UserDetails details) {
		logged = new User();
		logged.setUsername(details.getUsername());
		logged.setPassword(details.getPassword());
		Authorities aux = new Authorities();
		for (GrantedAuthority auth : details.getAuthorities()) {
			aux.setAuthority(auth.getAuthority());
		}
		logged.setAuthority(aux);
		return logged;
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldFindAll() throws Exception {
		User sara = new User();
		sara.setId(2);
		sara.setUsername("Sara");

		User juan = new User();
		juan.setId(3);
		juan.setUsername("Juan");

		when(this.userService.findAll()).thenReturn(List.of(user, sara, juan));

		mockMvc.perform(get(BASE_URL)).andExpect(status().isOk()).andExpect(jsonPath("$.size()").value(3))
				.andExpect(jsonPath("$[?(@.id == 1)].username").value("user"))
				.andExpect(jsonPath("$[?(@.id == 2)].username").value("Sara"))
				.andExpect(jsonPath("$[?(@.id == 3)].username").value("Juan"));
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldFindAllWithAuthority() throws Exception {
		Authorities aux = new Authorities();
		aux.setId(2);
		aux.setAuthority("AUX");

		User sara = new User();
		sara.setId(2);
		sara.setUsername("Sara");
		sara.setAuthority(aux);

		User juan = new User();
		juan.setId(3);
		juan.setUsername("Juan");
		juan.setAuthority(auth);

		when(this.userService.findAllByAuthority(auth.getAuthority())).thenReturn(List.of(user, juan));

		mockMvc.perform(get(BASE_URL).param("auth", "PLAYER")).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(2)).andExpect(jsonPath("$[?(@.id == 1)].username").value("user"))
				.andExpect(jsonPath("$[?(@.id == 3)].username").value("Juan"));
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldFindAllAuths() throws Exception {
		Authorities aux = new Authorities();
		aux.setId(2);
		aux.setAuthority("AUX");

		when(this.authService.findAll()).thenReturn(List.of(auth, aux));

		mockMvc.perform(get(BASE_URL + "/authorities")).andExpect(status().isOk())
				.andExpect(jsonPath("$.size()").value(2)).andExpect(jsonPath("$[?(@.id == 1)].authority").value("PLAYER"))
				.andExpect(jsonPath("$[?(@.id == 2)].authority").value("AUX"));
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldReturnUser() throws Exception {
		when(this.userService.findById(TEST_USER_ID)).thenReturn(user);
		mockMvc.perform(get(BASE_URL + "/{id}", TEST_USER_ID)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(TEST_USER_ID))
				.andExpect(jsonPath("$.username").value(user.getUsername()))
				.andExpect(jsonPath("$.authority.authority").value(user.getAuthority().getAuthority()));
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldReturnNotFoundUser() throws Exception {
		when(this.userService.findById(TEST_USER_ID)).thenThrow(ResourceNotFoundException.class);
		mockMvc.perform(get(BASE_URL + "/{id}", TEST_USER_ID)).andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldCreateUser() throws Exception {
		User aux = new User();
		aux.setUsername("Prueba");
		aux.setPassword("Prueba");
		aux.setAuthority(auth);

    when(this.userService.createAccount(any(User.class))).thenReturn(aux);
	mockMvc.perform(post(BASE_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(java.util.Map.of("username", "Prueba", "password", "Prueba", "authority", java.util.Map.of("id", TEST_AUTH_ID))))).andExpect(status().isCreated());
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldUpdateUser() throws Exception {
		user.setUsername("UPDATED");
		user.setPassword("CHANGED");

		when(this.userService.findById(TEST_USER_ID)).thenReturn(user);
		UserDTO dto = new UserDTO(user);
		when(this.userService.updateUser(any(UserDTO.class), any(Integer.class))).thenReturn(user);

		mockMvc.perform(put(BASE_URL + "/{id}", TEST_USER_ID).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("UPDATED")).andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldReturnNotFoundUpdateUser() throws Exception {
		user.setUsername("UPDATED");
		user.setPassword("UPDATED");
		UserDTO dto = new UserDTO(user);

		when(this.userService.findById(TEST_USER_ID)).thenThrow(ResourceNotFoundException.class);
		when(this.userService.updateUser(any(UserDTO.class), any(Integer.class))).thenReturn(user);

		mockMvc.perform(put(BASE_URL + "/{id}", TEST_USER_ID).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldDeleteOtherUser() throws Exception {
		logged.setId(2);

		when(this.userService.findById(TEST_USER_ID)).thenReturn(user);
		doNothing().when(this.userService).deleteUser(TEST_USER_ID);

		mockMvc.perform(delete(BASE_URL + "/{id}", TEST_USER_ID).with(csrf())).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("User deleted!"));
	}

	@Test
	@WithMockUser(username = "admin", authorities = "ADMIN")
	void shouldNotDeleteLoggedUser() throws Exception {
		logged.setId(TEST_USER_ID);

		when(this.userService.findById(TEST_USER_ID)).thenReturn(user);
		doNothing().when(this.userService).deleteUser(TEST_USER_ID);

		mockMvc.perform(delete(BASE_URL + "/{id}", TEST_USER_ID).with(csrf())).andExpect(status().isForbidden())
				.andExpect(result -> assertTrue(result.getResolvedException() instanceof AccessDeniedException));
	}

}
