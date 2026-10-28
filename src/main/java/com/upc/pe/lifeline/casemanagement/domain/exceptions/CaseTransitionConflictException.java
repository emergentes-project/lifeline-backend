package com.upc.pe.lifeline.casemanagement.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class CaseTransitionConflictException extends DomainException {
    public CaseTransitionConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
