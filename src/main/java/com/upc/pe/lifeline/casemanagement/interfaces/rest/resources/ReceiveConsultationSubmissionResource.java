package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReceiveConsultationSubmissionResource(UUID mobileConsultationId, UUID citizenUserId,
                                                    String inputText, String photoStorageKey,
                                                    boolean helpRequested, PatientResource patient,
                                                    AnswerResource answer,
                                                    LocalDateTime consultationRecordedAt,
                                                    UUID idempotencyKey) {
    public record PatientResource(String name, Integer ageYears, String locationDescription,
                                  String province, BigDecimal latitude, BigDecimal longitude,
                                  String phone) {
    }

    public record AnswerResource(String guidanceText, String guidanceStatus,
                                 String preliminaryPriority,
                                 boolean professionalHelpRecommended,
                                 List<Map<String, Object>> sourceReferences) {
    }
}
