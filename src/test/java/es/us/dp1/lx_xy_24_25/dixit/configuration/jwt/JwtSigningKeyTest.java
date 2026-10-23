package es.us.dp1.lx_xy_24_25.dixit.configuration.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.security.SecureRandom;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtSigningKeyTest {
    JwtUtils signer(String configured) {
        JwtUtils jwt=new JwtUtils(null);
        ReflectionTestUtils.setField(jwt,"jwtSecret",configured);
        jwt.configureSigningKey();return jwt;
    }
    String key(JwtUtils jwt){return (String)ReflectionTestUtils.getField(jwt,"jwtSecret");}
    @Test void noConfigurationGeneratesIndependentStrongKeysWithoutAPublishedFallback(){
        JwtUtils first=signer(""),second=signer(null);
        assertEquals(64,Base64.getDecoder().decode(key(first)).length);
        assertFalse(key(first).equals(key(second)),"Independent instances must not share a default key");
        String token=Jwts.builder().setSubject("disposable-fixture").setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(SignatureAlgorithm.HS512,key(first)).compact();
        assertTrue(first.validateJwtToken(token));assertFalse(second.validateJwtToken(token));
    }
    @Test void configuredKeyIsPreservedButMalformedAndShortConfigurationFailClosed(){
        byte[] bytes=new byte[64];new SecureRandom().nextBytes(bytes);
        String configured=Base64.getEncoder().encodeToString(bytes);
        assertTrue(configured.equals(key(signer(configured))));
        assertThrows(IllegalStateException.class,()->signer("not valid base64!"));
        assertThrows(IllegalStateException.class,()->signer(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
