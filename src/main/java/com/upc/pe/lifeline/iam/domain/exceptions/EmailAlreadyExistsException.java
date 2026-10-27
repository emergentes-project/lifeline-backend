package com.upc.pe.lifeline.iam.domain.exceptions;

public class EmailAlreadyExistsException extends IamConflictException {
    public EmailAlreadyExistsException(String email) {
        super("Email is already registered: " + email);
    }
}
