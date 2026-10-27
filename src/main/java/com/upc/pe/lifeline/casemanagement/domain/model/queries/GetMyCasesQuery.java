package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetMyCasesQuery(UUID currentCitizenUserId) {
    public GetMyCasesQuery {
        if (currentCitizenUserId == null) {
            throw new IllegalArgumentException("Citizen user ID is required");
        }
    }
}
