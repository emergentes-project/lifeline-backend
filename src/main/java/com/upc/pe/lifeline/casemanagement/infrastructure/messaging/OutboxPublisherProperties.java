package com.upc.pe.lifeline.casemanagement.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "lifeline.outbox")
public record OutboxPublisherProperties(
        String destination,
        int batchSize,
        Duration publishDelay,
        Duration claimLease,
        int maxAttempts) {

    public OutboxPublisherProperties {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("lifeline.outbox.destination is required");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("lifeline.outbox.batch-size must be at least 1");
        }
        if (publishDelay == null || publishDelay.isZero() || publishDelay.isNegative()) {
            throw new IllegalArgumentException("lifeline.outbox.publish-delay must be positive");
        }
        if (claimLease == null || claimLease.isZero() || claimLease.isNegative()) {
            throw new IllegalArgumentException("lifeline.outbox.claim-lease must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("lifeline.outbox.max-attempts must be at least 1");
        }
    }
}
