package com.upc.pe.lifeline.iam.domain.model.events;

import java.util.UUID;

public record MedicalProfessionalLicenseUpdatedEvent(UUID userId) {
    public MedicalProfessionalLicenseUpdatedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
