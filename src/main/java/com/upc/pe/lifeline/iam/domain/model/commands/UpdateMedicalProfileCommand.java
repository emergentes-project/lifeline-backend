package com.upc.pe.lifeline.iam.domain.model.commands;

import java.util.UUID;

public record UpdateMedicalProfileCommand(UUID userId, String professionalCategory,
                                          String professionalLicenseNumber, String specialty) {
    public UpdateMedicalProfileCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (professionalCategory == null || professionalCategory.isBlank()) {
            throw new IllegalArgumentException("Professional category is required");
        }
        if (professionalLicenseNumber == null || professionalLicenseNumber.isBlank()) {
            throw new IllegalArgumentException("Professional license number is required");
        }
    }
}
