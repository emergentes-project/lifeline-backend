package com.upc.pe.lifeline.iam.domain.model.commands;

public record RegisterMedicalProfessionalCommand(String email, String password, String firstName,
                                                 String lastName, String phone,
                                                 String professionalCategory,
                                                 String professionalLicenseNumber, String specialty) {
    public RegisterMedicalProfessionalCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (professionalCategory == null || professionalCategory.isBlank()) {
            throw new IllegalArgumentException("Professional category is required");
        }
        if (professionalLicenseNumber == null || professionalLicenseNumber.isBlank()) {
            throw new IllegalArgumentException("Professional license number is required");
        }
    }
}
