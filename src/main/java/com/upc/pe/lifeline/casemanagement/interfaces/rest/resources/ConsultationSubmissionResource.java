package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.util.UUID;

public record ConsultationSubmissionResource(UUID id, UUID idempotencyKey, boolean helpRequested) {
}
