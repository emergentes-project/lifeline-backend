package com.upc.pe.lifeline.casemanagement.domain.model.aggregates;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.Assignation;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
}
