package com.upc.pe.lifeline.shared.infrastructure.security;

import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final String issuer;
    private final Duration expiration;

    public JwtService(
            @Value("${authorization.jwt.secret}") String secret,
            @Value("${authorization.jwt.issuer:lifeline}") String issuer,
            @Value("${authorization.jwt.expiration:PT1H}") Duration expiration) {
        if (secret == null || secret.isBlank() || issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT secret and issuer are required");
        }
        if (expiration == null || expiration.toSeconds() <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public String generateToken(UserId userId, String role) {
        if (userId == null || role == null || !role.matches("[A-Z][A-Z0-9_]{0,39}")) {
            throw new IllegalArgumentException("User ID and role are required");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.value().toString())
                .claim("token_use", "access")
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateServiceToken(String serviceName) {
        if (serviceName == null || !serviceName.matches("[a-zA-Z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("A valid service name is required");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(serviceName)
                .claim("token_use", "service")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getExpiration() == null || claims.getIssuedAt() == null
                || !claims.getExpiration().after(claims.getIssuedAt())) {
            throw new MalformedJwtException("Invalid token lifetime");
        }
        String subject = claims.getSubject();
        String tokenUse = claims.get("token_use", String.class);
        if ("access".equals(tokenUse)) {
            if (subject == null || !UUID.fromString(subject).toString().equals(subject)) {
                throw new MalformedJwtException("Invalid token subject");
            }
            String role = claims.get("role", String.class);
            if (role == null || !role.matches("[A-Z][A-Z0-9_]{0,39}")) {
                throw new MalformedJwtException("Invalid token role");
            }
        } else if (!"service".equals(tokenUse) || subject == null
                || !subject.matches("[a-zA-Z0-9_-]{1,80}")) {
            throw new MalformedJwtException("Invalid token use");
        }
        return claims;
    }
}
