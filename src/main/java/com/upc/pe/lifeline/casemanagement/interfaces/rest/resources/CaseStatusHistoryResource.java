package com.upc.pe.lifeline.casemanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

public record CaseStatusHistoryResource(UUID id, UUID caseId, String fromStatus, String toStatus,
                                        String actorType, UUID userId, LocalDateTime occurredAt) {
}
