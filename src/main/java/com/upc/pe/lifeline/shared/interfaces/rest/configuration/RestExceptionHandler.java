package com.upc.pe.lifeline.shared.interfaces.rest.configuration;

import com.upc.pe.lifeline.shared.interfaces.rest.resources.MessageResource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<MessageResource> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception) {
        String message = "Unsupported Content-Type";
        if (!exception.getSupportedMediaTypes().isEmpty()) {
            message += ". Supported types: " + exception.getSupportedMediaTypes();
        }
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new MessageResource(message));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<MessageResource> handleMissingServletRequestPart(
            MissingServletRequestPartException exception) {
        String message = "Required multipart part '" + exception.getRequestPartName() + "' is missing";
        return ResponseEntity.badRequest().body(new MessageResource(message));
    }
}
