package com.riwi.helpflow.ticket.infrastructure.security;

import com.riwi.helpflow.ticket.domain.model.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JWT token issuance and validation.
 * No Spring context — tests the issuer directly.
 */
class JwtTokenIssuerAdapterTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!!";
    private JwtTokenIssuerAdapter issuer;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        issuer = new JwtTokenIssuerAdapter(SECRET, 60);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Issued token contains correct claims: sub, email, role")
    void issuedToken_hasCorrectClaims() {
        UUID userId = UUID.randomUUID();
        User user = User.create(userId, "DOC", "Name", "user@helpflow.com",
                "BOGOTA", "IT", Role.ROLE_USUARIO, UserStatus.ACTIVO, "hash");

        String token = issuer.issue(user);

        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@helpflow.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USUARIO");
        assertThat(claims.getId()).isNotNull(); // jti
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("Token signed with HS256 — invalid secret fails validation")
    void token_invalidSecretFails() {
        User user = User.create(UUID.randomUUID(), "DOC", "Name", "user@helpflow.com",
                null, null, Role.ROLE_ADMIN, UserStatus.ACTIVO, "hash");
        String token = issuer.issue(user);

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-at-least-32-bytes-long-!!!!!".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() ->
                Jwts.parser().verifyWith(wrongKey).build().parseSignedClaims(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Token with short expiration expires correctly")
    void token_shortExpiration() {
        JwtTokenIssuerAdapter shortLived = new JwtTokenIssuerAdapter(SECRET, 0); // 0 minutes
        User user = User.create(UUID.randomUUID(), "DOC", "Name", "user@helpflow.com",
                null, null, Role.ROLE_USUARIO, UserStatus.ACTIVO, "hash");

        String token = shortLived.issue(user);

        // Token should already be expired (0 minutes expiration)
        // Small race possible but unlikely
        assertThatThrownBy(() ->
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token))
                .isInstanceOf(Exception.class);
    }
}
