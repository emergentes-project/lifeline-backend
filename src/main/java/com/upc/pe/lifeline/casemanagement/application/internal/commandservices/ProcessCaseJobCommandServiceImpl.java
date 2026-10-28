package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.services.ProcessCaseJobCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProcessCaseJobCommandServiceImpl implements ProcessCaseJobCommandService {
    private final OutboxEventRepository events;
    private final ConsultationSubmissionRepository submissions;
    private final CaseRepository cases;
    private final ProcessCaseJobCommandServiceImpl self;

    public ProcessCaseJobCommandServiceImpl(OutboxEventRepository events,
                                            ConsultationSubmissionRepository submissions,
                                            CaseRepository cases,
                                            @Lazy ProcessCaseJobCommandServiceImpl self) {
        this.events = events;
        this.submissions = submissions;
        this.cases = cases;
        this.self = self;
    }

    @Override
    public Result handle(UUID jobId) {
        try {
            return self.create(jobId);
        } catch (DataIntegrityViolationException exception) {
            if (caseExists(jobId)) {
                return Result.ALREADY_PRESENT;
            }
            throw exception;
        }
    }

    @Transactional
    public Result create(UUID jobId) {
        OutboxEvent job = events.findById(jobId).orElseThrow(NoSuchElementException::new);
        ConsultationSubmission submission = submissions.findById(job.getAggregateId())
                .orElseThrow(NoSuchElementException::new);
        if (cases.findByConsultationSubmission_Id(submission.getId()).isPresent()) {
            return Result.ALREADY_PRESENT;
        }
        cases.saveAndFlush(new Case(submission));
        return Result.CREATED;
    }

    private boolean caseExists(UUID jobId) {
        return events.findById(jobId)
                .flatMap(job -> submissions.findById(job.getAggregateId()))
                .flatMap(submission -> cases.findByConsultationSubmission_Id(submission.getId()))
                .isPresent();
    }
}
