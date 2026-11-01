package com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    Optional<NotificationDelivery> findByCaseIdAndStatus(UUID caseId, String status);

    @Query("""
            select delivery.id from NotificationDelivery delivery
            where delivery.sentAt is null
              and delivery.attempts < :maxAttempts
              and delivery.nextAttemptAt <= :now
            """)
    List<UUID> findDueIds(@Param("maxAttempts") int maxAttempts, @Param("now") LocalDateTime now);
}
