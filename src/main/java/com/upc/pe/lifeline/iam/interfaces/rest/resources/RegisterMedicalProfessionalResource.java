package com.upc.pe.lifeline.iam.interfaces.rest.resources;

public record RegisterMedicalProfessionalResource(String email, String password, String firstName,
                                                  String lastName, String phone, String professionalCategory,
                                                  String professionalLicenseNumber, String specialty) {
}
