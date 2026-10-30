package com.upc.pe.lifeline.casemanagement.domain.model.aggregates;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.Assignation;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.domain.exceptions.CaseTransitionConflictException;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
public class Case extends AuditableAbstractAggregateRoot<Case> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private ConsultationSubmission consultationSubmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Column(columnDefinition = "text")
    private String observation;

    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "caseEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Assignation> assignations = new ArrayList<>();

    @OneToMany(mappedBy = "caseEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CaseStatusHistory> statusHistory = new ArrayList<>();

    public Case() {}

    public Case(ConsultationSubmission consultationSubmission) {
        this.consultationSubmission = consultationSubmission;
        this.status = CaseStatus.AVAILABLE;
    }

    public void recordCreation(LocalDateTime occurredAt) {
        statusHistory.add(new CaseStatusHistory(this, null, CaseStatus.AVAILABLE,
                "SYSTEM", null, occurredAt));
    }

    public void assignTo(UUID professionalUserId, LocalDateTime occurredAt) {
        if (status != CaseStatus.AVAILABLE) {
            throw new CaseTransitionConflictException("Only available cases can be assigned");
        }
        assignations.add(new Assignation(this, professionalUserId, occurredAt));
        changeStatus(CaseStatus.ASSIGNED, professionalUserId, occurredAt);
    }

    public void release(UUID professionalUserId, LocalDateTime occurredAt) {
        if (status != CaseStatus.ASSIGNED) {
            throw new CaseTransitionConflictException("Only assigned cases can be released");
        }
        activeAssignmentFor(professionalUserId).setClosedAt(occurredAt);
        changeStatus(CaseStatus.AVAILABLE, professionalUserId, occurredAt);
    }

    public void start(UUID professionalUserId, LocalDateTime occurredAt) {
        if (status != CaseStatus.ASSIGNED) {
            throw new CaseTransitionConflictException("Only assigned cases can be started");
        }
        activeAssignmentFor(professionalUserId);
        changeStatus(CaseStatus.IN_PROGRESS, professionalUserId, occurredAt);
    }

    public void close(UUID professionalUserId, String observation, LocalDateTime occurredAt) {
        if (status != CaseStatus.IN_PROGRESS) {
            throw new CaseTransitionConflictException("Only cases in progress can be closed");
        }
        activeAssignmentFor(professionalUserId).setClosedAt(occurredAt);
        this.observation = observation;
        this.closedAt = occurredAt;
        changeStatus(CaseStatus.CLOSED, professionalUserId, occurredAt);
    }

    private Assignation activeAssignmentFor(UUID professionalUserId) {
        return assignations.stream()
                .filter(assignment -> assignment.getClosedAt() == null
                        && assignment.getProfessionalUserId().userId().equals(professionalUserId))
                .findFirst()
                .orElseThrow(() -> new CaseTransitionConflictException("Case is not assigned to this professional"));
    }

    private void changeStatus(CaseStatus nextStatus, UUID userId, LocalDateTime occurredAt) {
        CaseStatus previous = this.status;
        this.status = nextStatus;
        statusHistory.add(new CaseStatusHistory(this, previous, nextStatus,
                "PROFESSIONAL", userId, occurredAt));
    }
}
