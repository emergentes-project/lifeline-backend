package com.upc.pe.lifeline.iam.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class UserNotCreatedException extends DomainException {
    public UserNotCreatedException(Throwable cause) {
        super("User registration could not be completed", HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
