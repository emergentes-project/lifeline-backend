package com.upc.pe.lifeline.iam.domain.model.commands;

import java.util.UUID;

public record SignOutUserCommand(UUID userId, String accessToken) {
    public SignOutUserCommand {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }
    }
}
