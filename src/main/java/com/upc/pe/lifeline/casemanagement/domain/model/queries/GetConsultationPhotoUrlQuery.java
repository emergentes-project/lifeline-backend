package com.upc.pe.lifeline.casemanagement.domain.model.queries;

import java.util.UUID;

public record GetConsultationPhotoUrlQuery(UUID consultationId) {
    public GetConsultationPhotoUrlQuery {
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation ID is required");
        }
    }
}
