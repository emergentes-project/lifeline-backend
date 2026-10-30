package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.ConsultationAnswer;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Patient;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ReceiveConsultationSubmissionCommandServiceImpl implements ReceiveConsultationSubmissionCommandService {
    private final ConsultationSubmissionRepository submissions;
    private final OutboxEventRepository events;
    private final ReceiveConsultationSubmissionCommandServiceImpl self;

    public ReceiveConsultationSubmissionCommandServiceImpl(ConsultationSubmissionRepository submissions,
                                                           OutboxEventRepository events,
                                                           @Lazy ReceiveConsultationSubmissionCommandServiceImpl self) {
        this.submissions = submissions;
        this.events = events;
        this.self = self;
    }

    @Override
    public Reception handle(ReceiveConsultationSubmissionCommand command) {
        return submissions.findByIdempotencyKey(command.idempotencyKey())
                .map(Reception::replay)
                .orElseGet(() -> insertOrReplay(command));
    }

    private Reception insertOrReplay(ReceiveConsultationSubmissionCommand command) {
        try {
            return self.insert(command);
        } catch (DataIntegrityViolationException exception) {
            return submissions.findByIdempotencyKey(command.idempotencyKey())
                    .map(Reception::replay)
                    .orElseThrow(() -> exception);
        }
    }

    @Transactional
    public Reception insert(ReceiveConsultationSubmissionCommand command) {
        return submissions.findByIdempotencyKey(command.idempotencyKey())
                .map(Reception::replay)
                .orElseGet(() -> persist(command));
    }

    private Reception persist(ReceiveConsultationSubmissionCommand command) {
        LocalDateTime receivedAt = LocalDateTime.now(ZoneOffset.UTC);
        ConsultationSubmission submission = new ConsultationSubmission(
                command.mobileConsultationId(),
                command.citizenUserId(),
                command.inputText(),
                null,
                command.helpRequested(),
                command.consultationRecordedAt(),
                receivedAt,
                command.idempotencyKey(),
                patient(command.patient()),
                answer(command.answer()));
        submission.setRequesterPhone(command.requesterPhone() != null ? command.requesterPhone()
                : command.patient() == null ? null : command.patient().phone());
        submissions.saveAndFlush(submission);
        if (command.helpRequested() && !submission.isPhotoPending()) {
            events.saveAndFlush(ConsultationReceivedOutboxEventFactory.create(submission));
        }
        return Reception.created(submission);
    }

    private Patient patient(ReceiveConsultationSubmissionCommand.PatientData data) {
        if (data == null) {
            return null;
        }
        return new Patient(null, data.name(), data.ageYears(), data.locationDescription(),
                data.province(), data.latitude(), data.longitude(), data.phone());
    }

    private ConsultationAnswer answer(ReceiveConsultationSubmissionCommand.AnswerData data) {
        if (data == null) {
            return null;
        }
        return new ConsultationAnswer(null, data.guidanceText(), data.guidanceStatus(),
                data.preliminaryPriority(), data.professionalHelpRecommended(), data.sourceReferences());
    }
}
