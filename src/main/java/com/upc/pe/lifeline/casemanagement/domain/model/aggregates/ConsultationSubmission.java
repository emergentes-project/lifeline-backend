package com.upc.pe.lifeline.casemanagement.domain.model.aggregates;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.ConsultationAnswer;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Patient;
import com.upc.pe.lifeline.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
public class ConsultationSubmission extends AuditableAbstractAggregateRoot<ConsultationSubmission> {
    @Column(nullable = false, unique = true)
    private UUID mobileConsultationId;

    @Embedded
    @AttributeOverride(name = "userId", column = @Column(name = "citizen_user_id"))
    private UserId citizenUserId;

    @Column(columnDefinition = "text")
    private String inputText;

    @Column(columnDefinition = "text")
    private String photoStorageKey;

    @Column(nullable = false)
    private boolean helpRequested;

    @Column(nullable = false)
    private LocalDateTime consultationRecordedAt;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false, unique = true)
    private UUID idempotencyKey;

    @OneToOne(mappedBy = "consultationSubmission", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Patient patient;

    @OneToOne(mappedBy = "consultationSubmission", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private ConsultationAnswer answer;

    public ConsultationSubmission() {}

    public ConsultationSubmission(UUID mobileConsultationId, UUID citizenUserId, String inputText,
                                  String photoStorageKey, boolean helpRequested,
                                  LocalDateTime consultationRecordedAt, LocalDateTime receivedAt,
                                  UUID idempotencyKey, Patient patient, ConsultationAnswer answer) {
        if (mobileConsultationId == null) {
            throw new IllegalArgumentException("Mobile consultation ID is required");
        }
        this.mobileConsultationId = mobileConsultationId;
        this.citizenUserId = citizenUserId == null ? null : new UserId(citizenUserId);
        this.inputText = inputText;
        this.photoStorageKey = photoStorageKey;
        this.helpRequested = helpRequested;
        this.consultationRecordedAt = consultationRecordedAt;
        this.receivedAt = receivedAt;
        this.idempotencyKey = idempotencyKey;
        this.patient = patient;
        this.answer = answer;
        if (patient != null) {
            patient.setConsultationSubmission(this);
        }
        if (answer != null) {
            answer.setConsultationSubmission(this);
        }
    }
}
