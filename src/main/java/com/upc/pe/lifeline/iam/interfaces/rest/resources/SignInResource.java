package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignInResource(@Schema(example = "ana@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
                             @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String password) {
}
