package com.upc.pe.lifeline.casemanagement.domain.model.entities;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.GuidanceStatus;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.PreliminaryPriority;
import com.upc.pe.lifeline.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
public class ConsultationAnswer extends AuditableModel {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private ConsultationSubmission consultationSubmission;

    @Column(columnDefinition = "text")
    private String guidanceText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuidanceStatus guidanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PreliminaryPriority preliminaryPriority;

    @Column(nullable = false)
    private boolean professionalHelpRecommended;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> sourceReferences = new ArrayList<>();

    public ConsultationAnswer() {}

    public ConsultationAnswer(ConsultationSubmission consultationSubmission,
                              String guidanceText, GuidanceStatus guidanceStatus,
                              PreliminaryPriority preliminaryPriority, boolean professionalHelpRecommended,
                              List<Map<String, Object>> sourceReferences) {
        this.consultationSubmission = consultationSubmission;
        this.guidanceText = guidanceText;
        this.guidanceStatus = guidanceStatus;
        this.preliminaryPriority = preliminaryPriority;
        this.professionalHelpRecommended = professionalHelpRecommended;
        this.sourceReferences = sourceReferences == null ? new ArrayList<>() : new ArrayList<>(sourceReferences);
    }
}
