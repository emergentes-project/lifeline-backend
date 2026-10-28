package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterMedicalProfessionalResource(
        @Schema(example = "doctor@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String password,
        @Schema(example = "Luis", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(example = "Ruiz", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        String phone,
        @Schema(example = "DOCTOR", allowableValues = {"DOCTOR", "NURSE", "PARAMEDIC", "FIREFIGHTER",
                "MEDICAL_INTERN", "MEDICAL_RESIDENT"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String professionalCategory,
        @Schema(example = "A3480045365123", requiredMode = Schema.RequiredMode.REQUIRED)
        String professionalLicenseNumber,
        String specialty) {
}
