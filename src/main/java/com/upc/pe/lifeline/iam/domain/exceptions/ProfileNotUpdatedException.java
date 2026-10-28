package com.upc.pe.lifeline.iam.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class ProfileNotUpdatedException extends DomainException {
    public ProfileNotUpdatedException(Throwable cause) {
        super("Profile update could not be completed", HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
