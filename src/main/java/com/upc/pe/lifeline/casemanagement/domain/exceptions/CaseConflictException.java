package com.upc.pe.lifeline.casemanagement.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CaseConflictException extends DomainException {
    public CaseConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
