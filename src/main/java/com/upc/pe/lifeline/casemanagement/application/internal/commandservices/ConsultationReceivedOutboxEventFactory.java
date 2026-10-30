package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;

import java.util.LinkedHashMap;
import java.util.Map;

final class ConsultationReceivedOutboxEventFactory {
    private static final String CONSULTATION_RECEIVED = "consultation.received";

    private ConsultationReceivedOutboxEventFactory() {
    }

    static OutboxEvent create(ConsultationSubmission submission) {
        OutboxEvent event = new OutboxEvent(submission.getId(), CONSULTATION_RECEIVED, Map.of());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", event.getId().toString());
        payload.put("eventType", CONSULTATION_RECEIVED);
        payload.put("aggregateId", submission.getId().toString());
        payload.put("idempotencyKey", submission.getIdempotencyKey().toString());
        event.setPayload(payload);
        return event;
    }
}
