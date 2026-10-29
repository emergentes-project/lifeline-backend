package com.upc.pe.lifeline.casemanagement.domain.model.aggregates;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.Assignation;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import com.upc.pe.lifeline.casemanagement.domain.exceptions.CaseConflictException;
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

    public void assignTo(UUID professionalUserId, LocalDateTime occurredAt) {
        if (status != CaseStatus.AVAILABLE) {
            throw new CaseConflictException("The case is not available for assignment");
        }
        status = CaseStatus.ASSIGNED;
        assignations.add(new Assignation(this, professionalUserId, occurredAt));
        statusHistory.add(new CaseStatusHistory(this, CaseStatus.AVAILABLE, CaseStatus.ASSIGNED,
                "PROFESSIONAL", professionalUserId, occurredAt));
    }

    public void startCare(UUID professionalUserId, LocalDateTime occurredAt) {
        if (status != CaseStatus.ASSIGNED || !openAssignationBelongsTo(professionalUserId)) {
            throw new CaseConflictException("The case is not assigned to this professional");
        }
        status = CaseStatus.IN_PROGRESS;
        statusHistory.add(new CaseStatusHistory(this, CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS,
                "PROFESSIONAL", professionalUserId, occurredAt));
    }

    private boolean openAssignationBelongsTo(UUID professionalUserId) {
        return assignations.stream()
                .filter(assignation -> assignation.getClosedAt() == null)
                .anyMatch(assignation -> assignation.getProfessionalUserId().userId().equals(professionalUserId));
    }
}
