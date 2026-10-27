package com.upc.pe.lifeline.iam.domain.model.events;

import java.util.UUID;

public record MedicalProfessionalRegisteredEvent(UUID userId) {
    public MedicalProfessionalRegisteredEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
