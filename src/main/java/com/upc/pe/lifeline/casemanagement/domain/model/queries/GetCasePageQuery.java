package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetCasePageQuery(String status, String assignmentRelation,
                               String priority, String province,
                               UUID professionalUserId, UUID citizenUserId,
                               int page, int size) {
    public GetCasePageQuery {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Invalid page or size");
        }
        if ((professionalUserId == null) == (citizenUserId == null)) {
            throw new IllegalArgumentException("Exactly one user scope is required");
        }
    }
}
