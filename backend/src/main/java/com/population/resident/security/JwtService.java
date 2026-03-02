package com.population.resident.security;

import com.population.resident.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, List<String> roles, String currentRole) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(jwtProperties.getExpireSeconds());
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claims(Map.of(
                        "uid", userId,
                        "roles", roles,
                        "currentRole", currentRole
                ))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getExpireSeconds() {
        return jwtProperties.getExpireSeconds();
    }
}
