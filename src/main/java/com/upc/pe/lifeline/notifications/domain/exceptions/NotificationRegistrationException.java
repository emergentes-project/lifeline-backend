package com.upc.pe.lifeline.notifications.domain.exceptions;

import com.upc.pe.lifeline.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class NotificationRegistrationException extends DomainException {
    public NotificationRegistrationException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
