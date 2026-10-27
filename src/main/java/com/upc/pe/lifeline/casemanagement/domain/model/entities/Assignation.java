package com.upc.pe.lifeline.casemanagement.domain.model.entities;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
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
public class Assignation extends AuditableModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Embedded
    @AttributeOverride(name = "userId", column = @Column(name = "professional_user_id", nullable = false))
    private UserId professionalUserId;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime closedAt;

    public Assignation() {}

    public Assignation(Case caseEntity, UUID professionalUserId, LocalDateTime assignedAt) {
        this.caseEntity = caseEntity;
        this.professionalUserId = new UserId(professionalUserId);
        this.assignedAt = assignedAt;
    }
}
