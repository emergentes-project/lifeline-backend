package com.upc.pe.lifeline.iam.domain.model.valueobjects;

import java.util.Locale;

public enum ProfessionalCategory {
    DOCTOR,
    NURSE,
    PARAMEDIC,
    FIREFIGHTER,
    MEDICAL_INTERN,
    MEDICAL_RESIDENT;

    public static ProfessionalCategory from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Professional category is required");
        }
        return valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    public String value() {
        return name();
    }
}
