package com.upc.pe.lifeline.iam.domain.exceptions;

import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends DomainException {
    public RoleNotFoundException(RolesCode code) {
        super("Required role is missing: " + code, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
