package org.misha.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecurityTest {
    private static final String SECRET = "test-only-signing-key-32-bytes-minimum-never-use-in-production";
    private final JwtUtil jwt = new JwtUtil(SECRET, 900000, 604800000);

    @Test void validAccessHasSubjectAndRole() {
        var claims = jwt.validateAccessClaims(jwt.generateAccessToken("1", "ADMIN"));
        assertEquals("1", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test void refreshCannotAuthenticateAnApiRequest() {
        assertThrows(JwtException.class, () -> jwt.validateAccessClaims(jwt.generateRefreshToken("1")));
    }

    @Test void accessCannotRotateARefreshToken() {
        assertThrows(JwtException.class, () -> jwt.validateRefreshToken(jwt.generateAccessToken("1", "ADMIN")));
    }

    @Test void rejectsExpiredTamperedMissingExpiryAndInvalidSubject() {
        assertThrows(JwtException.class, () -> jwt.validateAccessClaims(signed("1", new Date(1))));
        assertThrows(JwtException.class, () -> jwt.validateAccessClaims(signed("1", null)));
        assertThrows(JwtException.class, () -> jwt.validateAccessClaims(signed("anonymous", new Date(System.currentTimeMillis() + 60000))));
        var foreign = new JwtUtil("different-test-signing-key-at-least-32-bytes", 1000, 2000);
        assertThrows(JwtException.class, () -> jwt.validateAccessClaims(foreign.generateAccessToken("1", "ADMIN")));
    }

    private String signed(String subject, Date expires) {
        return Jwts.builder().setSubject(subject).claim("type", "access").setExpiration(expires)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
    }
}
