package com.upc.pe.lifeline.casemanagement.domain.services;

import java.util.UUID;

public interface ProcessCaseJobCommandService {
    Result handle(UUID jobId);

    enum Result {
        CREATED,
        ALREADY_PRESENT
    }
}
