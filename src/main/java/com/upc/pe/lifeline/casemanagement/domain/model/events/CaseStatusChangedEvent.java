package com.upc.pe.lifeline.casemanagement.domain.model.events;

import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;

import java.util.UUID;

public record CaseStatusChangedEvent(UUID caseId, CaseStatus previousStatus, CaseStatus newStatus) {
    public CaseStatusChangedEvent {
        if (caseId == null || previousStatus == null || newStatus == null) {
            throw new IllegalArgumentException("Case status change is incomplete");
        }
    }
}
