package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateMedicalProfileResource(
        @Schema(example = "DOCTOR", requiredMode = Schema.RequiredMode.REQUIRED) String professionalCategory,
        @Schema(example = "A3480045365123", requiredMode = Schema.RequiredMode.REQUIRED)
        String professionalLicenseNumber,
        String specialty) {
}
