package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReceiveConsultationSubmissionCommand(UUID mobileConsultationId, UUID citizenUserId,
                                                   String inputText, String photoStorageKey,
                                                   boolean helpRequested, PatientData patient,
                                                   AnswerData answer,
                                                   LocalDateTime consultationRecordedAt,
                                                   UUID idempotencyKey) {
    public ReceiveConsultationSubmissionCommand {
        if (mobileConsultationId == null) {
            throw new IllegalArgumentException("Mobile consultation ID is required");
        }
        if (consultationRecordedAt == null) {
            throw new IllegalArgumentException("Consultation recorded time is required");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if ((inputText == null || inputText.isBlank())
                && (photoStorageKey == null || photoStorageKey.isBlank())) {
            throw new IllegalArgumentException("Consultation text or photo is required");
        }
    }

    public record PatientData(String name, Integer ageYears, String locationDescription,
                              String province, BigDecimal latitude, BigDecimal longitude) {
    }

    public record AnswerData(String guidanceText, String guidanceStatus,
                             String preliminaryPriority,
                             boolean professionalHelpRecommended,
                             List<Map<String, Object>> sourceReferences) {
        public AnswerData {
            if (guidanceStatus == null || guidanceStatus.isBlank()) {
                throw new IllegalArgumentException("Guidance status is required");
            }
            sourceReferences = sourceReferences == null ? List.of() : List.copyOf(sourceReferences);
        }
    }
}
