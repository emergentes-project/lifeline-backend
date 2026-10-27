package com.upc.pe.lifeline.iam.domain.model.commands;

import java.util.UUID;

public record UpdateCitizenProfileCommand(UUID userId, String city, String district,
                                          String address, String emergencyContactName,
                                          String emergencyContactPhone,
                                          String preExistingConditions, String allergies) {
    public UpdateCitizenProfileCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (district == null || district.isBlank()) {
            throw new IllegalArgumentException("District is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (emergencyContactName != null && emergencyContactName.isBlank()) {
            throw new IllegalArgumentException("Emergency contact name must not be blank");
        }
        if (emergencyContactPhone != null && emergencyContactPhone.isBlank()) {
            throw new IllegalArgumentException("Emergency contact phone must not be blank");
        }
        if (emergencyContactName == null && emergencyContactPhone != null) {
            throw new IllegalArgumentException("Emergency contact name is required when a phone is provided");
        }
        if (emergencyContactPhone == null && emergencyContactName != null) {
            throw new IllegalArgumentException("Emergency contact phone is required when a name is provided");
        }
    }
}
