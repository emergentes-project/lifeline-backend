package com.upc.pe.lifeline.notifications.interfaces.rest.controllers;

import com.upc.pe.lifeline.notifications.domain.services.NotificationTokenCommandService;
import com.upc.pe.lifeline.notifications.interfaces.rest.resources.RegisterNotificationTokenResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/notification-tokens", consumes = APPLICATION_JSON_VALUE)
public class NotificationTokenController {
    private final NotificationTokenCommandService notificationTokens;

    public NotificationTokenController(NotificationTokenCommandService notificationTokens) {
        this.notificationTokens = notificationTokens;
    }

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody RegisterNotificationTokenResource resource,
                                         Authentication authentication) {
        notificationTokens.register(resource.token(), resource.consultationId(), citizenUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID citizenUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        boolean citizen = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CITIZEN".equals(authority.getAuthority()));
        if (!citizen) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
        return UUID.fromString(authentication.getName());
    }
}
