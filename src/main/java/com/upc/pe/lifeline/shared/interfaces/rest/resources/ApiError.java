package com.upc.pe.lifeline.shared.interfaces.rest.resources;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, List<ValidationError> errors) {
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, List.of());
    }

    public record ValidationError(String field, String message) {
    }
}
