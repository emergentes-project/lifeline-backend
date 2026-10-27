package com.upc.pe.lifeline.iam.domain.model.commands;

public record SignInUserCommand(String email, String password) {
    public SignInUserCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
