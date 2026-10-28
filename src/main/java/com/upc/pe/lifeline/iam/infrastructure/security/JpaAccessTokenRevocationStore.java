package com.upc.pe.lifeline.iam.infrastructure.security;

import com.upc.pe.lifeline.iam.domain.model.entities.RevokedAccessToken;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.RevokedAccessTokenRepository;
import com.upc.pe.lifeline.shared.application.ports.AccessTokenRevocationStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class JpaAccessTokenRevocationStore implements AccessTokenRevocationStore {
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public JpaAccessTokenRevocationStore(RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return revokedAccessTokenRepository.existsById(hash(token));
    }

    @Override
    @Transactional
    public void revoke(String token, Instant expiresAt) {
        revokedAccessTokenRepository.deleteExpired(Instant.now());
        revokedAccessTokenRepository.saveAndFlush(new RevokedAccessToken(hash(token), expiresAt));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
