package com.upc.pe.lifeline.iam.interfaces.rest.resources;

import java.util.UUID;

public record UserResource(UUID id, String email, String firstName, String lastName,
                           String phone, String role) {
}
