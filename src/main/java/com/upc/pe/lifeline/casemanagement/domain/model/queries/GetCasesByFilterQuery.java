package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetCasesByFilterQuery(String status, String assignmentRelation,
                                    UUID currentProfessionalUserId) {
    public GetCasesByFilterQuery {
        if (status != null && status.isBlank()) {
            throw new IllegalArgumentException("Status must not be blank");
        }
        if (assignmentRelation != null && assignmentRelation.isBlank()) {
            throw new IllegalArgumentException("Assignment relation must not be blank");
        }
        if (currentProfessionalUserId == null) {
            throw new IllegalArgumentException("Professional user ID is required");
        }
    }
}
