package com.upc.pe.lifeline.shared.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record UserId(UUID userId) {
    public UserId {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
