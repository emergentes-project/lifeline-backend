package com.upc.pe.lifeline.iam.domain.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class RevokedAccessToken {
    @Id
    @Column(length = 64, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedAccessToken() {
    }

    public RevokedAccessToken(String tokenHash, Instant expiresAt) {
        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Token hash is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Token expiration is required");
        }
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
