package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetCaseStatusHistoryByCaseIdQuery(UUID caseId) {
    public GetCaseStatusHistoryByCaseIdQuery {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
    }
}
