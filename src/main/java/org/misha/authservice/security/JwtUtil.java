package org.misha.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access.expiration}") long accessExpiration,
            @Value("${jwt.refresh.expiration}") long refreshExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (accessExpiration <= 0 || refreshExpiration <= 0) {
            throw new IllegalArgumentException("Token expiration must be positive");
        }
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String subject) {
        return generateAccessToken(subject, null);
    }

    public String generateAccessToken(String subject, String role) {
        var builder = Jwts.builder()
                .setSubject(subject)
                .claim("type", "access")
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public String getRoleClaim(String token) {
        return (String) parse(token).getBody().get("role");
    }

    public String generateRefreshToken(String subject) {
        String jti = java.util.UUID.randomUUID().toString();
        return generateRefreshToken(subject, jti);
    }

    public String generateRefreshToken(String subject, String jti) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", "refresh")
                .setId(jti)
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public String validateAccessToken(String token) {
        return validateClaims(token, "access").getSubject();
    }

    public Claims validateAccessClaims(String token) {
        return validateClaims(token, "access");
    }

    public String validateRefreshToken(String token) {
        return validateClaims(token, "refresh").getSubject();
    }

    private Claims validateClaims(String token, String type) {
        Claims claims = parse(token).getBody();
        if (!type.equals(claims.get("type")) || claims.getExpiration() == null
                || claims.getSubject() == null) throw new JwtException("Invalid token claims");
        try {
            if (Long.parseLong(claims.getSubject()) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new JwtException("Invalid token subject");
        }
        return claims;
    }

    public String getJti(String token) {
        return parse(token).getBody().getId();
    }
}
