package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConsultationSubmissionDetailResource(UUID id, UUID mobileConsultationId, UUID citizenUserId,
                                                   String inputText, boolean hasPhoto, boolean helpRequested,
                                                   LocalDateTime consultationRecordedAt, LocalDateTime receivedAt,
                                                   PatientResource patient, AnswerResource answer) {
    public record PatientResource(String name, Integer ageYears, String locationDescription,
                                  String province, BigDecimal latitude, BigDecimal longitude) {
    }

    public record AnswerResource(String guidanceText, String guidanceStatus, String preliminaryPriority,
                                 boolean professionalHelpRecommended,
                                 List<Map<String, Object>> sourceReferences) {
    }
}
