package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record RegisterCitizenResource(@Schema(example = "ana@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
                                      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String password,
                                      @Schema(example = "Ana", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
                                      @Schema(example = "Pérez", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
                                      String phone,
                                      @Schema(example = "Lima", requiredMode = Schema.RequiredMode.REQUIRED) String city,
                                      @Schema(example = "Miraflores", requiredMode = Schema.RequiredMode.REQUIRED) String district,
                                      @Schema(example = "Av. Principal 123", requiredMode = Schema.RequiredMode.REQUIRED) String address,
                                      @Schema(example = "1995-01-20", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dateOfBirth,
                                      String emergencyContactName, String emergencyContactPhone) {
}
