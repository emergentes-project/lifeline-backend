package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.infrastructure.messaging.CaseJobMessageSender;
import com.upc.pe.lifeline.casemanagement.infrastructure.messaging.OutboxPublisherProperties;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxClaimStore;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxClaimStore claims;
    private final OutboxEventRepository events;
    private final CaseJobMessageSender sender;
    private final OutboxPublisherProperties properties;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxClaimStore claims, OutboxEventRepository events, CaseJobMessageSender sender,
                           OutboxPublisherProperties properties, ObjectMapper objectMapper) {
        this.claims = claims;
        this.events = events;
        this.sender = sender;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publishPending() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        try {
            claims.releaseExpired(now.minus(properties.claimLease()), now);
            for (UUID id : claims.claim(now, properties.batchSize())) {
                publishOne(id);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Outbox claim failed", exception);
        }
    }

    private void publishOne(UUID id) {
        try {
            OutboxEvent event = events.findById(id).orElse(null);
            if (event == null) {
                return;
            }
            String body = message(event);
            sender.send(body);
            claims.recordSuccess(id, LocalDateTime.now(ZoneOffset.UTC));
        } catch (RuntimeException exception) {
            LOGGER.warn("Outbox event {} was not published", id);
            try {
                claims.recordFailure(id, LocalDateTime.now(ZoneOffset.UTC),
                        properties.publishDelay(), properties.maxAttempts());
            } catch (RuntimeException failure) {
                LOGGER.warn("Outbox event {} could not be marked for retry", id);
            }
        }
    }

    private String message(OutboxEvent event) {
        Map<String, Object> payload = event.getPayload();
        String aggregateId = required(payload, "aggregateId");
        String idempotencyKey = required(payload, "idempotencyKey");
        required(payload, "jobId");
        required(payload, "eventType");
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        Map<String, String> message = new LinkedHashMap<>();
        message.put("jobId", event.getId().toString());
        message.put("eventType", event.getEventType());
        message.put("aggregateId", aggregateId);
        message.put("idempotencyKey", idempotencyKey);
        return objectMapper.writeValueAsString(message);
    }

    private String required(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }
}
