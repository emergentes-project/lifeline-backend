package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalProfileResource(UUID id, UUID userId, String professionalCategory,
                                     String professionalLicenseNumber, String specialty,
                                     String verificationStatus, LocalDateTime verifiedAt) {
}
