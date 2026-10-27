package com.upc.pe.lifeline.iam.domain.model.commands;

import java.time.LocalDate;

public record RegisterCitizenCommand(String email, String password, String firstName,
                                     String lastName, String phone, String city,
                                     String district, String address, LocalDate dateOfBirth,
                                     String emergencyContactName, String emergencyContactPhone) {
    public RegisterCitizenCommand {
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
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (district == null || district.isBlank()) {
            throw new IllegalArgumentException("District is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
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
