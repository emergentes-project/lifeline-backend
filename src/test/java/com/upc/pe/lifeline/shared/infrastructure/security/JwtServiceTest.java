package com.upc.pe.lifeline.shared.infrastructure.security;

import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private final JwtService service = new JwtService(SECRET, "lifeline", Duration.ofHours(1));

    @Test
    void accessTokenPreservesIdentityRoleAndExpiration() {
        UserId userId = new UserId(UUID.randomUUID());
        Instant before = Instant.now();

        var claims = service.parseClaims(service.generateToken(userId, "CITIZEN"));

        assertThat(claims.getSubject()).isEqualTo(userId.value().toString());
        assertThat(claims.get("role")).isEqualTo("CITIZEN");
        assertThat(claims.get("token_use")).isEqualTo("access");
        assertThat(claims.getExpiration().toInstant()).isAfter(before.plusSeconds(3598));
    }

    @Test
    void rejectsExpiredTokens() {
        String token = signedToken("lifeline", Instant.now().minusSeconds(30));

        assertThatThrownBy(() -> service.parseClaims(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        String otherSecret = Base64.getEncoder().encodeToString("different-secret-with-32-bytes!!!".getBytes());
        JwtService otherService = new JwtService(otherSecret, "lifeline", Duration.ofHours(1));
        String token = otherService.generateToken(new UserId(UUID.randomUUID()), "CITIZEN");

        assertThatThrownBy(() -> service.parseClaims(token)).isInstanceOf(JwtException.class);
    }

    private String signedToken(String issuer, Instant expiration) {
        return Jwts.builder().issuer(issuer).subject(UUID.randomUUID().toString())
                .claim("token_use", "access").claim("role", "CITIZEN")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
    }
}
