package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService.Reception;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultation_reception;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
class ReceiveConsultationSubmissionCommandServiceImplTest {
    @Autowired
    private ReceiveConsultationSubmissionCommandServiceImpl service;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @Autowired
    private OutboxEventRepository events;

    @MockitoSpyBean
    private OutboxEventRepository eventSpy;

    @Test
    void helpRequestedPersistsOneSubmissionAndOnePendingEvent() {
        ReceiveConsultationSubmissionCommand command = command(true, UUID.randomUUID(), UUID.randomUUID(), "dolor");

        Reception reception = service.handle(command);

        assertThat(reception.created()).isTrue();
        List<OutboxEvent> storedEvents = eventsFor(reception.submission().getId());
        assertThat(storedEvents).hasSize(1);
        OutboxEvent event = storedEvents.getFirst();
        assertThat(event.getAggregateId()).isEqualTo(reception.submission().getId());
        assertThat(event.getEventType()).isEqualTo("consultation.received");
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getNextAttemptAt()).isNull();
        assertThat(event.getPayload()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "jobId", event.getId().toString(),
                "eventType", "consultation.received",
                "aggregateId", reception.submission().getId().toString(),
                "idempotencyKey", command.idempotencyKey().toString()));
        assertThat(submissions.findByIdempotencyKey(command.idempotencyKey())).isPresent();
    }

    @Test
    void helpNotRequestedPersistsTheSubmissionWithoutAnEvent() {
        ReceiveConsultationSubmissionCommand command = command(false, UUID.randomUUID(), UUID.randomUUID(), "consulta");

        Reception reception = service.handle(command);

        assertThat(reception.created()).isTrue();
        assertThat(reception.submission().isHelpRequested()).isFalse();
        assertThat(eventsFor(reception.submission().getId())).isEmpty();
    }

    @Test
    void repeatingTheIdempotencyKeyReturnsTheStoredSubmissionAndDoesNotAddAnotherEvent() {
        UUID key = UUID.randomUUID();
        UUID mobileId = UUID.randomUUID();
        ReceiveConsultationSubmissionCommand first = command(true, mobileId, key, "primera");
        Reception created = service.handle(first);

        Reception replay = service.handle(command(true, UUID.randomUUID(), key, "segunda"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.submission().getId()).isEqualTo(created.submission().getId());
        assertThat(replay.submission().getInputText()).isEqualTo("primera");
        assertThat(replay.submission().getMobileConsultationId()).isEqualTo(mobileId);
        assertThat(eventsFor(created.submission().getId())).hasSize(1);
    }

    @Test
    void repeatingTheIdempotencyKeyDoesNotCreateAnEventWhenTheFirstRequestDidNotAskForHelp() {
        UUID key = UUID.randomUUID();
        Reception created = service.handle(command(false, UUID.randomUUID(), key, "sin ayuda"));

        Reception replay = service.handle(command(true, UUID.randomUUID(), key, "ahora si"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.submission().isHelpRequested()).isFalse();
        assertThat(eventsFor(created.submission().getId())).isEmpty();
    }

    @Test
    void aDifferentIdempotencyKeyWithTheSameMobileIdStaysAConflict() {
        UUID mobileId = UUID.randomUUID();
        service.handle(command(true, mobileId, UUID.randomUUID(), "original"));

        assertThatThrownBy(() -> service.handle(command(true, mobileId, UUID.randomUUID(), "otra clave")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(submissions.findAll().stream()
                .filter(submission -> mobileId.equals(submission.getMobileConsultationId()))).hasSize(1);
    }

    @Test
    void aFailureWhileSavingTheEventRollsBackTheSubmission() {
        doThrow(new IllegalStateException("outbox unavailable")).when(eventSpy).saveAndFlush(any());
        UUID key = UUID.randomUUID();

        assertThatThrownBy(() -> service.handle(command(true, UUID.randomUUID(), key, "revierte")))
                .hasRootCauseInstanceOf(IllegalStateException.class);

        assertThat(submissions.findByIdempotencyKey(key)).isEmpty();
    }

    private List<OutboxEvent> eventsFor(UUID aggregateId) {
        return events.findAll().stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .toList();
    }

    private static ReceiveConsultationSubmissionCommand command(boolean helpRequested, UUID mobileId,
                                                                UUID idempotencyKey, String inputText) {
        return new ReceiveConsultationSubmissionCommand(mobileId, null, inputText, null, helpRequested,
                null, null, LocalDateTime.now(ZoneOffset.UTC), idempotencyKey);
    }
}
