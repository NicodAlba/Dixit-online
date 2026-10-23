package es.us.dp1.lx_xy_24_25.dixit.configuration.jwt;

import java.util.Date;
import java.util.Base64;
import java.security.SecureRandom;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import es.us.dp1.lx_xy_24_25.dixit.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import es.us.dp1.lx_xy_24_25.dixit.configuration.services.UserDetailsImpl;
import es.us.dp1.lx_xy_24_25.dixit.user.Authorities;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Component
public class JwtUtils {
	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private final UserService userService;

    @Value("${dp1.game.app.jwtSecret:}")
	private String jwtSecret;

	@Value("${dp1.game.app.jwtExpirationMs}")
	private int jwtExpirationMs;

    public JwtUtils(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    void configureSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            byte[] key = new byte[64];
            new SecureRandom().nextBytes(key);
            jwtSecret = Base64.getEncoder().encodeToString(key);
            logger.info("Using an ephemeral JWT signing key; sessions expire on restart. Configure DIXIT_JWT_SECRET to retain the key.");
            return;
        }
        try {
            if (Base64.getDecoder().decode(jwtSecret).length >= 64) return;
        } catch (IllegalArgumentException ignored) { }
        throw new IllegalStateException("DIXIT_JWT_SECRET must encode at least 64 random bytes in Base64.");
    }

    public String generateJwtToken(Authentication authentication) {
		UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

		Map<String, Object> claims = new HashMap<>();
		claims.put("authorities",
				userPrincipal.getAuthorities().stream().map(auth -> auth.getAuthority()).collect(Collectors.toList()));
		// añadir id del usuario al payload
		claims.put("id", userPrincipal.getId());

		return Jwts.builder().setClaims(claims).setSubject((userPrincipal.getUsername())).setIssuedAt(now)
				.setExpiration(expiryDate).signWith(SignatureAlgorithm.HS512, jwtSecret).compact();
	}

	public String generateTokenFromUsername(String username, Authorities authority) {
        UserDetailsImpl userPrincipal = UserDetailsImpl.build(userService.findCurrentUser()); //esto
        Map<String, Object> claims = new HashMap<>();
		claims.put("authorities", authority.getAuthority());
		claims.put("id", userPrincipal.getId().toString());
		return Jwts.builder().setClaims(claims).setSubject(username).setIssuedAt(new Date())
				.setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
				.signWith(SignatureAlgorithm.HS512, jwtSecret).compact();
	}

	public String getUserNameFromJwtToken(String token) {
		return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (SignatureException e) {
			logger.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}
}