package com.upc.pe.lifeline.iam.domain.exceptions;

public class ProfessionalLicenseAlreadyExistsException extends IamConflictException {
    public ProfessionalLicenseAlreadyExistsException(String licenseNumber) {
        super("Professional license number is already registered: " + licenseNumber);
    }
}
