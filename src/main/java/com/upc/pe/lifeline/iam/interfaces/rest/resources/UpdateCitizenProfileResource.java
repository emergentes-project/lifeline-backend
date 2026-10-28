package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateCitizenProfileResource(
        @Schema(example = "Lima", requiredMode = Schema.RequiredMode.REQUIRED) String city,
        @Schema(example = "Miraflores", requiredMode = Schema.RequiredMode.REQUIRED) String district,
        @Schema(example = "Av. Principal 123", requiredMode = Schema.RequiredMode.REQUIRED) String address,
        String emergencyContactName,
        String emergencyContactPhone,
        String preExistingConditions,
        String allergies) {
}
