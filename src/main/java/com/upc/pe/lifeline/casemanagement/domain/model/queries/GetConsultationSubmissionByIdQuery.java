package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetConsultationSubmissionByIdQuery(UUID consultationId) {
    public GetConsultationSubmissionByIdQuery {
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation ID is required");
        }
    }
}
