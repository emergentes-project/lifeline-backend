package com.upc.pe.lifeline.notifications.interfaces.rest.resources;

import java.util.UUID;

public record RegisterNotificationTokenResource(String token, UUID consultationId) {
}
