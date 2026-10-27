package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.util.UUID;

public record AssignCaseCommand(UUID caseId, UUID professionalUserId) {
    public AssignCaseCommand {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID is required");
        }
        if (professionalUserId == null) {
            throw new IllegalArgumentException("Professional user ID is required");
        }
    }
}
