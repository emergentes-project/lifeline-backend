package com.upc.pe.lifeline.shared.infrastructure.security;

import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        var claims = service.parseClaims(service.generateToken(userId, "CITIZEN"));

        assertThat(claims.getSubject()).isEqualTo(userId.userId().toString());
        assertThat(claims.get("role")).isEqualTo("CITIZEN");
        assertThat(claims.get("token_use")).isEqualTo("access");
        assertThat(LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneOffset.UTC))
                .isAfter(before.plusSeconds(3598));
    }

    @Test
    void rejectsExpiredTokens() {
        String token = signedToken("lifeline", LocalDateTime.now(ZoneOffset.UTC).minusSeconds(30));

        assertThatThrownBy(() -> service.parseClaims(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        String otherSecret = Base64.getEncoder().encodeToString("different-secret-with-32-bytes!!!".getBytes());
        JwtService otherService = new JwtService(otherSecret, "lifeline", Duration.ofHours(1));
        String token = otherService.generateToken(new UserId(UUID.randomUUID()), "CITIZEN");

        assertThatThrownBy(() -> service.parseClaims(token)).isInstanceOf(JwtException.class);
    }

    private String signedToken(String issuer, LocalDateTime expiration) {
        return Jwts.builder().issuer(issuer).subject(UUID.randomUUID().toString())
                .claim("token_use", "access").claim("role", "CITIZEN")
                .issuedAt(Date.from(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(120).toInstant(ZoneOffset.UTC)))
                .expiration(Date.from(expiration.toInstant(ZoneOffset.UTC)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
    }
}
