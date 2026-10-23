package com.upc.pe.lifeline.shared.domain.model.valueobjects;

import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
