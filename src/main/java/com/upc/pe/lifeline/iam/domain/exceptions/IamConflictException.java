package com.upc.pe.lifeline.iam.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class IamConflictException extends DomainException {
    public IamConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public IamConflictException(String message, Throwable cause) {
        this(message);
        initCause(cause);
    }
}
