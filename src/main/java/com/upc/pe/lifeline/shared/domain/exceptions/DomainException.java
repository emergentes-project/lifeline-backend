package com.upc.pe.lifeline.shared.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;

    protected DomainException(String message, HttpStatus status) {
        super(message);
        if (message == null || message.isBlank() || status == null || !status.isError()) {
            throw new IllegalArgumentException("A domain error message and an error HTTP status are required");
        }
        this.status = status;
    }
}
