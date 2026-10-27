package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.util.UUID;

public record CreateCaseFromSubmissionCommand(UUID consultationId) {
    public CreateCaseFromSubmissionCommand {
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation ID is required");
        }
    }
}
