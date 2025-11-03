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
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("type", "access")
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
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
        Claims claims = parse(token).getBody();
        if (!"access".equals(claims.get("type"))) throw new JwtException("Invalid token type");
        return claims.getSubject();
    }

    public String validateRefreshToken(String token) {
        Claims claims = parse(token).getBody();
        if (!"refresh".equals(claims.get("type"))) throw new JwtException("Invalid token type");
        return claims.getSubject();
    }

    public String getJti(String token) {
        return parse(token).getBody().getId();
    }
}