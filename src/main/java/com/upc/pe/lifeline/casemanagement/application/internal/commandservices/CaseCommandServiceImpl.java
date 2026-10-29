package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.AssignCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ChangeCaseStatusCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

@Service
public class CaseCommandServiceImpl implements CaseCommandService {
    private final CaseRepository cases;
    private final ApplicationEventPublisher events;

    public CaseCommandServiceImpl(CaseRepository cases, ApplicationEventPublisher events) {
        this.cases = cases;
        this.events = events;
    }

    @Override
    @Transactional
    public Case handle(AssignCaseCommand command) {
        Case caseEntity = cases.findById(command.caseId())
                .orElseThrow(() -> new NoSuchElementException("Case not found"));
        caseEntity.assignTo(command.professionalUserId(), LocalDateTime.now(ZoneOffset.UTC));
        events.publishEvent(new CaseStatusChangedEvent(caseEntity.getId(), CaseStatus.AVAILABLE, caseEntity.getStatus()));
        loadForResponse(caseEntity);
        return caseEntity;
    }

    @Override
    @Transactional
    public Case handle(ChangeCaseStatusCommand command) {
        if (!"IN_PROGRESS".equals(command.newStatus())) {
            throw new IllegalArgumentException("Only IN_PROGRESS is accepted");
        }
        Case caseEntity = cases.findById(command.caseId())
                .orElseThrow(() -> new NoSuchElementException("Case not found"));
        caseEntity.startCare(command.userId(), LocalDateTime.now(ZoneOffset.UTC));
        events.publishEvent(new CaseStatusChangedEvent(caseEntity.getId(), CaseStatus.ASSIGNED, caseEntity.getStatus()));
        loadForResponse(caseEntity);
        return caseEntity;
    }

    private void loadForResponse(Case caseEntity) {
        caseEntity.getConsultationSubmission().getId();
        caseEntity.getAssignations().size();
        caseEntity.getCreatedAt();
    }
}
