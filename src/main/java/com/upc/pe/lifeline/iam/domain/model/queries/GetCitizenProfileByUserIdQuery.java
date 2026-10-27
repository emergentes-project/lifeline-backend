package com.upc.pe.lifeline.iam.domain.model.queries;

import java.util.UUID;

public record GetCitizenProfileByUserIdQuery(UUID userId) {
    public GetCitizenProfileByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
