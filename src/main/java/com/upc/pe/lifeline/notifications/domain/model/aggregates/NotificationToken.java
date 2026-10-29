package com.upc.pe.lifeline.notifications.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notification_tokens")
@Check(constraints = "user_id IS NOT NULL OR consultation_id IS NOT NULL")
public class NotificationToken {
    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "consultation_id", columnDefinition = "uuid")
    private UUID consultationId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NotificationToken() {
    }

    public NotificationToken(String token, UUID userId, UUID consultationId, LocalDateTime updatedAt) {
        this.id = UUID.randomUUID();
        this.token = token;
        replaceOwner(userId, consultationId, updatedAt);
    }

    public void replaceOwner(UUID userId, UUID consultationId, LocalDateTime updatedAt) {
        if (userId == null && consultationId == null) {
            throw new IllegalArgumentException("Notification token owner is required");
        }
        this.userId = userId;
        this.consultationId = consultationId;
        this.updatedAt = updatedAt;
    }
}
