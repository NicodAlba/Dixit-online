package es.us.dp1.lx_xy_24_25.dixit.configuration;

import es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.AuthEntryPointJwt;
import es.us.dp1.lx_xy_24_25.dixit.configuration.jwt.AuthTokenFilter;
import es.us.dp1.lx_xy_24_25.dixit.configuration.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Autowired
	UserDetailsServiceImpl userDetailsService;

	@Autowired
	private AuthEntryPointJwt unauthorizedHandler;

	@Autowired
	DataSource dataSource;

	private static final String ADMIN = "ADMIN";
	private static final String PLAYER = "PLAYER";


	@Bean
	protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

		http
            .cors(withDefaults())
            // Si usas H2, conviene ignorar su CSRF:
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))

            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos comunes (css, js, images, webjars…) públicos
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                // H2 Console accesible
                .requestMatchers(PathRequest.toH2Console()).permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Raíz / páginas públicas
                .requestMatchers("/", "/oups").permitAll()

                // Swagger / OpenAPI accesible
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**"
                ).permitAll()

                .requestMatchers("/ws/**").permitAll()
                // API pública
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/developers").permitAll()
                .requestMatchers("/api/v1/plan").permitAll()
                .requestMatchers("/api/v1/users/**").permitAll()

                // API restringida para administradores
                .requestMatchers(HttpMethod.GET, "/api/v1/games").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/games").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers("/api/v1/games/my").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers("/api/v1/games/{id}").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/games/{id}/join").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/games/{id}/leave").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/games/{id}/start").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/turns/{id}").hasAnyAuthority(PLAYER,ADMIN)
                .requestMatchers(HttpMethod.PUT, "api/v1/turns/{turnId}/answer/{cardId}").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.PUT, "api/v1/turns/{turnId}/clue").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.POST, "api/v1/turns/{turnId}/play-card/{cardId}/{playerId}").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.POST, "api/v1/turns/{turnId}/vote/{playerId}/{cardId}").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/global").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/rankings/**").hasAnyAuthority(PLAYER, ADMIN)

                // API for players
                .requestMatchers(HttpMethod.GET, "api/v1/friends").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "api/v1/friends/pending").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.PATCH, "api/v1/friends/{requestId}/accept").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.DELETE, "api/v1/friends/{requestId}/deny").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.DELETE, "api/v1/friends/{userId}/remove").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.POST, "api/v1/friends/addFriend/{userId}").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.POST, "api/v1/games/{gameId}/invite").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "api/v1/games/invitations").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.PATCH, "api/v1/games/invitations/{invitationId}/accept").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "/api/v1/achievements").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}/achievements").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/user/{userId}").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "/api/v1/messages/games/**").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/v1/messages/games/**").hasAnyAuthority(PLAYER, ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/games/{gameId}/spectate/{playerId}").hasAuthority(PLAYER)
                .requestMatchers(HttpMethod.GET, "/api/v1/players/{playerId}").hasAuthority(PLAYER)
                

                // El resto denegado
                .anyRequest().denyAll()
            )


			.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public AuthTokenFilter authenticationJwtTokenFilter() {
		return new AuthTokenFilter();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}



}
