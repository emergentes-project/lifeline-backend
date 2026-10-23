package com.upc.pe.lifeline.iam.domain.model.valueobjects;

public record EmergencyContact(String name, String phone) {
    public EmergencyContact {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Emergency contact name is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Emergency contact phone is required");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Emergency contact name must not exceed 200 characters");
        }
    }
}
