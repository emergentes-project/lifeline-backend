package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetCaseByIdQuery(UUID caseId) {
    public GetCaseByIdQuery {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
    }
}
