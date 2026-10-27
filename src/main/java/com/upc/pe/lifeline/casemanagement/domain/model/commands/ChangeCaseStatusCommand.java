package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.util.UUID;

public record ChangeCaseStatusCommand(UUID caseId, String newStatus, UUID userId) {
    public ChangeCaseStatusCommand {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("New status is required");
        }
    }
}
