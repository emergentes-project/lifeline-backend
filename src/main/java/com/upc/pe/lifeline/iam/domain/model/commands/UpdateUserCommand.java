package com.upc.pe.lifeline.iam.domain.model.commands;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String firstName, String lastName, String phone) {
    public UpdateUserCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }
}
