package com.upc.pe.lifeline.iam.domain.model.queries;

import java.util.UUID;

public record GetMedicalProfileByUserIdQuery(UUID userId) {
    public GetMedicalProfileByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
