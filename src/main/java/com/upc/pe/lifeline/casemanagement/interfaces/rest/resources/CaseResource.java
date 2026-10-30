package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record CaseResource(UUID id, UUID consultationId, UUID citizenUserId, String status,
                           UUID assignedProfessionalUserId, String observation, LocalDateTime closedAt,
                           LocalDateTime createdAt) {
}
