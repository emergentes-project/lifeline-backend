package com.upc.pe.lifeline.notifications.domain.services;

import java.util.UUID;

public interface NotificationTokenCommandService {
    void register(String token, UUID consultationId, UUID citizenUserId);

    void unregister(String token, UUID citizenUserId);
}
