package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.util.UUID;

public record CloseCaseCommand(UUID caseId, String observation, UUID userId) {
    public CloseCaseCommand {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}
