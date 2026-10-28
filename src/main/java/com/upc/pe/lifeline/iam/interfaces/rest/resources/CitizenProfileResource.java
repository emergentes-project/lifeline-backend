package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

public record CitizenProfileResource(UUID id, UUID userId, String city, String district,
                                     String address, LocalDate dateOfBirth,
                                     String emergencyContactName, String emergencyContactPhone,
                                     String preExistingConditions, String allergies) {
}
