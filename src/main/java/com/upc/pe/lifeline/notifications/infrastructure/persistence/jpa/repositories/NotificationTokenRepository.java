package com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, UUID> {
    Optional<NotificationToken> findByToken(String token);

    List<NotificationToken> findByUserId(UUID userId);

    List<NotificationToken> findByConsultationId(UUID consultationId);
}
