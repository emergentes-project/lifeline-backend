package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import java.time.LocalDate;

public record RegisterCitizenResource(String email, String password, String firstName,
                                      String lastName, String phone, String city,
                                      String district, String address, LocalDate dateOfBirth,
                                      String emergencyContactName, String emergencyContactPhone) {
}
