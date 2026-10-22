package es.us.dp1.lx_xy_24_25.dixit.auth;

import es.us.dp1.lx_xy_24_25.dixit.achievement.AchievementService;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.request.LoginRequest;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.request.SignupRequest;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.JwtResponse;
import es.us.dp1.lx_xy_24_25.dixit.auth.payload.response.MessageResponse;
import es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.JwtUtils;
import es.us.dp1.lx_xy_24_25.dixit.configuration.services.UserDetailsImpl;
import es.us.dp1.lx_xy_24_25.dixit.exceptions.ResourceNotFoundException;
import es.us.dp1.lx_xy_24_25.dixit.user.User;
import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "The Authentication API based on JWT")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final UserService userService;
	private final JwtUtils jwtUtils;
	private final AuthService authService;
	private final AchievementService achievementService;

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtUtils jwtUtils,
			AuthService authService, AchievementService achievementService) {
		this.userService = userService;
		this.jwtUtils = jwtUtils;
		this.authenticationManager = authenticationManager;
		this.authService = authService;
		this.achievementService = achievementService;
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		try {
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());
			User user = userService.findCurrentUser();
			if (user.getAchievements().isEmpty() && user.hasAuthority("PLAYER")) {
				achievementService.initializeWinAchievements(user);
				achievementService.initializePlayAchievements(user);
				achievementService.initializeTrickAchievements(user);
			}
			return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), roles));
		} catch (BadCredentialsException exception) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new MessageResponse("Bad Credentials!"));
		}
	}

	@GetMapping("/validate")
	public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
		Boolean isValid = jwtUtils.validateJwtToken(token);
		return ResponseEntity.ok(isValid);
	}


	@PostMapping("/signup")
	public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (Boolean.TRUE.equals(userService.existsUser(signUpRequest.getUsername()))) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new MessageResponse("Error: Username is already taken!"));
		}
		try {
			authService.createUser(signUpRequest);
			return ResponseEntity.status(HttpStatus.CREATED)
				.body(new MessageResponse("User registered successfully!"));
		} catch (ResourceNotFoundException exception) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new MessageResponse(exception.getMessage()));
		}
	}

}
