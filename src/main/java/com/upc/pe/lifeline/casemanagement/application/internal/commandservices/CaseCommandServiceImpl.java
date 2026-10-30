package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.application.ports.ProfessionalEligibilityChecker;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.AssignCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ChangeCaseStatusCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.CloseCaseCommand;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.ApplicationEventPublisher;
import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CaseCommandServiceImpl implements CaseCommandService {
    private final CaseRepository cases;
    private final ProfessionalEligibilityChecker eligibility;
    private final ApplicationEventPublisher events;

    public CaseCommandServiceImpl(CaseRepository cases, ProfessionalEligibilityChecker eligibility,
                                  ApplicationEventPublisher events) {
        this.cases = cases;
        this.eligibility = eligibility;
        this.events = events;
    }

    @Override
    @Transactional
    public Case handle(AssignCaseCommand command) {
        requireApproved(command.professionalUserId());
        Case caseEntity = caseForUpdate(command.caseId());
        CaseStatus previous = caseEntity.getStatus();
        caseEntity.assignTo(command.professionalUserId(), now());
        return persistTransition(caseEntity, previous);
    }

    @Override
    @Transactional
    public Case handle(ChangeCaseStatusCommand command) {
        Case caseEntity = caseForUpdate(command.caseId());
        CaseStatus previous = caseEntity.getStatus();
        CaseStatus nextStatus = CaseStatus.valueOf(command.newStatus().strip().toUpperCase(Locale.ROOT));
        if (nextStatus == CaseStatus.AVAILABLE) {
            caseEntity.release(command.userId(), now());
        } else if (nextStatus == CaseStatus.IN_PROGRESS) {
            requireApproved(command.userId());
            caseEntity.start(command.userId(), now());
        } else {
            throw new IllegalArgumentException("Use the assignment or closure endpoint for this status");
        }
        return persistTransition(caseEntity, previous);
    }

    @Override
    @Transactional
    public Case handle(CloseCaseCommand command) {
        requireApproved(command.userId());
        Case caseEntity = caseForUpdate(command.caseId());
        CaseStatus previous = caseEntity.getStatus();
        caseEntity.close(command.userId(), command.observation(), now());
        return persistTransition(caseEntity, previous);
    }

    private Case persistTransition(Case caseEntity, CaseStatus previous) {
        Case saved = cases.saveAndFlush(caseEntity);
        events.publishEvent(new CaseStatusChangedEvent(saved.getId(), previous, saved.getStatus()));
        return saved;
    }

    private Case caseForUpdate(UUID caseId) {
        return cases.findByIdForUpdate(caseId).orElseThrow(() -> new NoSuchElementException("Case not found"));
    }

    private void requireApproved(UUID professionalUserId) {
        if (!eligibility.isApproved(professionalUserId)) {
            throw new AccessDeniedException("An approved medical profile is required");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
