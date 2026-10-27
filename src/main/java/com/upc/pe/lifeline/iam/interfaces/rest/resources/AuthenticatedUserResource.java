package com.upc.pe.lifeline.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(UserResource user, String token) {
}
