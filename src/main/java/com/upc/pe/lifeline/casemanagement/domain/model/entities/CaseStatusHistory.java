package com.upc.pe.lifeline.casemanagement.domain.model.entities;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
public class CaseStatusHistory extends AuditableModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus toStatus;

    @Column(nullable = false, length = 16)
    private String actorType;

    @Embedded
    @AttributeOverride(name = "userId", column = @Column(name = "user_id"))
    private UserId userId;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    public CaseStatusHistory() {}

    public CaseStatusHistory(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus,
                             String actorType, UUID userId, LocalDateTime occurredAt) {
        this.caseEntity = caseEntity;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorType = actorType;
        this.userId = userId == null ? null : new UserId(userId);
        this.occurredAt = occurredAt;
    }
}
