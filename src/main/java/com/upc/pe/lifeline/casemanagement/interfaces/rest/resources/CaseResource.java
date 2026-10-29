package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record CaseResource(UUID id, UUID consultationId, String status, UUID assignedProfessionalUserId,
                           String observation, LocalDateTime createdAt, LocalDateTime closedAt) {
}
