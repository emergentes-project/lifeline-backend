package com.upc.pe.lifeline.notifications.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notification_deliveries", uniqueConstraints = @UniqueConstraint(
        name = "notification_deliveries_case_status_uk", columnNames = {"case_id", "status"}))
public class NotificationDelivery {
    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "case_id", nullable = false, columnDefinition = "uuid")
    private UUID caseId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(UUID caseId, String status, LocalDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.status = status;
        this.attempts = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
    }

    public void markSent(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public void recordFailure(LocalDateTime nextAttemptAt) {
        this.attempts = this.attempts + 1;
        this.nextAttemptAt = nextAttemptAt;
    }
}
