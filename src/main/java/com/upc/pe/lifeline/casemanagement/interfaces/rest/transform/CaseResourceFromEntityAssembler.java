package com.upc.pe.lifeline.casemanagement.interfaces.rest.transform;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseResource;

import java.util.UUID;

public class CaseResourceFromEntityAssembler {
    public static CaseResource toResourceFromEntity(Case caseEntity) {
        return new CaseResource(
                caseEntity.getId(),
                caseEntity.getConsultationSubmission().getId(),
                caseEntity.getStatus().name(),
                openProfessional(caseEntity),
                caseEntity.getObservation(),
                caseEntity.getCreatedAt(),
                caseEntity.getClosedAt());
    }

    private static UUID openProfessional(Case caseEntity) {
        return caseEntity.getAssignations().stream()
                .filter(assignation -> assignation.getClosedAt() == null)
                .map(assignation -> assignation.getProfessionalUserId().userId())
                .findFirst()
                .orElse(null);
    }
}
