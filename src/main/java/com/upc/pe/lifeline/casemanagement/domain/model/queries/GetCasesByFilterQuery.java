package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetCasesByFilterQuery(String status, AssignmentRelation assignmentRelation,
                                    UUID currentProfessionalUserId) {
    public GetCasesByFilterQuery {
        if (currentProfessionalUserId == null) {
            throw new IllegalArgumentException("Professional user ID is required");
        }
    }

    public enum AssignmentRelation {
        ASSIGNED_TO_ME,
        NOT_ASSIGNED_TO_ME
    }
}
