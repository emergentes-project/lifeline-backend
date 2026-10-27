package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities;

import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
public class OutboxEvent extends AuditableModel {

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private int attempts;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime publishedAt;

    public OutboxEvent() {}

    public OutboxEvent(UUID aggregateId, String eventType, Map<String, Object> payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        this.status = "PENDING";
        this.attempts = 0;
    }
}
