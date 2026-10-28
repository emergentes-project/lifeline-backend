package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.infrastructure.messaging.CaseJobMessageSender;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:outbox_publisher;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
class OutboxPublisherTest {
    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private OutboxEventRepository events;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CaseJobMessageSender sender;

    @BeforeEach
    void clearEvents() {
        events.deleteAll();
    }

    @Test
    void writesPublishedAtOnlyAfterTheSendReturns() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        OutboxEvent event = pending(aggregateId, idempotencyKey, "from-payload");

        publisher.publishPending();

        OutboxEvent stored = events.findById(event.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo("PUBLISHED");
        assertThat(stored.getPublishedAt()).isNotNull();
        var body = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sender).send(body.capture());
        Map<String, String> message = objectMapper.readValue(body.getValue(), Map.class);
        assertThat(message).containsOnlyKeys("jobId", "eventType", "aggregateId", "idempotencyKey");
        assertThat(message.get("jobId")).isEqualTo(event.getId().toString());
        assertThat(message.get("eventType")).isEqualTo("consultation.received");
        assertThat(message.get("aggregateId")).isEqualTo(aggregateId.toString());
        assertThat(message.get("idempotencyKey")).isEqualTo(idempotencyKey.toString());
    }

    @Test
    void leavesPublishedAtEmptyWhenTheSendFails() {
        OutboxEvent event = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        doThrow(new RuntimeException("broker down")).when(sender).send(anyString());

        publisher.publishPending();

        OutboxEvent stored = events.findById(event.getId()).orElseThrow();
        assertThat(stored.getPublishedAt()).isNull();
        assertThat(stored.getStatus()).isEqualTo("PENDING");
        assertThat(stored.getAttempts()).isEqualTo(1);
        assertThat(stored.getNextAttemptAt()).isNotNull();
    }

    @Test
    void stopsClaimingAFailedOrPublishedRowAndReclaimsAnExpiredPublishingRow() {
        OutboxEvent published = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        published.setStatus("PUBLISHED");
        published.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
        events.saveAndFlush(published);
        OutboxEvent failed = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        failed.setStatus("FAILED");
        failed.setAttempts(5);
        events.saveAndFlush(failed);
        OutboxEvent fresh = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        markPublishing(fresh.getId(), LocalDateTime.now(ZoneOffset.UTC));
        OutboxEvent expired = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        markPublishing(expired.getId(), LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3));

        publisher.publishPending();

        assertThat(events.findById(published.getId()).orElseThrow().getStatus()).isEqualTo("PUBLISHED");
        assertThat(events.findById(failed.getId()).orElseThrow().getStatus()).isEqualTo("FAILED");
        assertThat(events.findById(fresh.getId()).orElseThrow().getStatus()).isEqualTo("PUBLISHING");
        OutboxEvent reclaimed = events.findById(expired.getId()).orElseThrow();
        assertThat(reclaimed.getStatus()).isEqualTo("PUBLISHED");
        assertThat(reclaimed.getPublishedAt()).isNotNull();
        verify(sender, never()).send(org.mockito.ArgumentMatchers.contains(published.getId().toString()));
        verify(sender, never()).send(org.mockito.ArgumentMatchers.contains(failed.getId().toString()));
        verify(sender, never()).send(org.mockito.ArgumentMatchers.contains(fresh.getId().toString()));
        verify(sender).send(org.mockito.ArgumentMatchers.contains(expired.getId().toString()));
    }

    @Test
    void theAttemptLimitLeavesTheRowFailedWithoutAPublishedAt() {
        OutboxEvent event = pending(UUID.randomUUID(), UUID.randomUUID(), "consultation.received");
        event.setAttempts(4);
        events.saveAndFlush(event);
        doThrow(new RuntimeException("broker down")).when(sender).send(anyString());

        publisher.publishPending();

        OutboxEvent stored = events.findById(event.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo("FAILED");
        assertThat(stored.getAttempts()).isEqualTo(5);
        assertThat(stored.getPublishedAt()).isNull();
    }

    @Test
    void aRowMissingAPayloadFieldIsNotSentAndCountsAsAFailedAttempt() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "consultation.received", Map.of(
                "jobId", UUID.randomUUID().toString(),
                "eventType", "consultation.received",
                "aggregateId", UUID.randomUUID().toString()));
        events.saveAndFlush(event);

        publisher.publishPending();

        verifyNoInteractions(sender);
        OutboxEvent stored = events.findById(event.getId()).orElseThrow();
        assertThat(stored.getPublishedAt()).isNull();
        assertThat(stored.getStatus()).isEqualTo("PENDING");
        assertThat(stored.getAttempts()).isEqualTo(1);
    }

    private OutboxEvent pending(UUID aggregateId, UUID idempotencyKey, String payloadEventType) {
        OutboxEvent event = new OutboxEvent(aggregateId, "consultation.received", Map.of());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", event.getId().toString());
        payload.put("eventType", payloadEventType);
        payload.put("aggregateId", aggregateId.toString());
        payload.put("idempotencyKey", idempotencyKey.toString());
        event.setPayload(payload);
        return events.saveAndFlush(event);
    }

    private void markPublishing(UUID id, LocalDateTime updatedAt) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.createNativeQuery("""
                    update outbox_events
                    set status = 'PUBLISHING', updated_at = :updatedAt
                    where id = :id
                    """)
                    .setParameter("updatedAt", updatedAt)
                    .setParameter("id", id)
                    .executeUpdate();
            entityManager.clear();
        });
    }
}
