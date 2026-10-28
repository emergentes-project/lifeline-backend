package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateUserResource(
        @Schema(example = "Ana", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(example = "Pérez", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        String phone) {
}
