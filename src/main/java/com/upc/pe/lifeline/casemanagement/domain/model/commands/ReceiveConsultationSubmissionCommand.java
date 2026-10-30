package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReceiveConsultationSubmissionCommand(UUID mobileConsultationId, UUID citizenUserId,
                                                   String inputText,
                                                   boolean helpRequested, PatientData patient,
                                                   AnswerData answer,
                                                   LocalDateTime consultationRecordedAt,
                                                   UUID idempotencyKey,
                                                   String requesterPhone) {
    public ReceiveConsultationSubmissionCommand(UUID mobileConsultationId, UUID citizenUserId,
                                                String inputText, boolean helpRequested, PatientData patient,
                                                AnswerData answer, LocalDateTime consultationRecordedAt,
                                                UUID idempotencyKey) {
        this(mobileConsultationId, citizenUserId, inputText, helpRequested, patient,
                answer, consultationRecordedAt, idempotencyKey, null);
    }

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
        requesterPhone = requesterPhone == null || requesterPhone.isBlank()
                ? null : requesterPhone.strip();
        if (requesterPhone != null && !requesterPhone.matches("\\+?[0-9 ()-]{6,30}")) {
            throw new IllegalArgumentException("Requester phone is invalid");
        }
    }

    public record PatientData(String name, Integer ageYears, String locationDescription,
                              String province, BigDecimal latitude, BigDecimal longitude,
                              String phone) {
        public PatientData(String name, Integer ageYears, String locationDescription,
                           String province, BigDecimal latitude, BigDecimal longitude) {
            this(name, ageYears, locationDescription, province, latitude, longitude, null);
        }

        public PatientData {
            phone = normalizePhone(phone);
        }

        private static String normalizePhone(String phone) {
            if (phone == null || phone.isBlank()) {
                return null;
            }
            String trimmed = phone.strip();
            if (trimmed.length() > 30) {
                throw new IllegalArgumentException("Patient phone must not exceed 30 characters");
            }
            return trimmed;
        }
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
