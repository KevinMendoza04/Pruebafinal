package com.riwi.helpflow.ticket.infrastructure.security;

import com.riwi.helpflow.ticket.application.port.out.TokenIssuerPort;
import com.riwi.helpflow.ticket.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token issuer using HS256. Secret from env JWT_SECRET (min 32 bytes).
 * Claims: sub (user id), email, role, iat, exp, jti.
 * Real validation filter + SecurityConfig added in PROMPT 4.
 */
@Component
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenIssuerAdapter(
            @Value("${helpflow.jwt.secret:dummy-secret-change-me-at-least-32-bytes!!!}") String secret,
            @Value("${helpflow.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String issue(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMinutes * 60_000))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }
}
