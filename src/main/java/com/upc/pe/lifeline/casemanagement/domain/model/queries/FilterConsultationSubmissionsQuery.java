package com.upc.pe.lifeline.casemanagement.domain.model.queries;

public record FilterConsultationSubmissionsQuery(String province, Boolean helpRequested) {
    public FilterConsultationSubmissionsQuery {
        if (province != null && province.isBlank()) {
            throw new IllegalArgumentException("Province must not be blank");
        }
        province = province == null ? null : province.strip();
    }
}
