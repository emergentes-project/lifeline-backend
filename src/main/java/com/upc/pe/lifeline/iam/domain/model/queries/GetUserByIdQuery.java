package com.upc.pe.lifeline.iam.domain.model.queries;

import java.util.UUID;

public record GetUserByIdQuery(UUID userId) {
    public GetUserByIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
