package com.upc.pe.lifeline.casemanagement.interfaces.rest.transform;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Assignation;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseResource;

public class CaseResourceFromEntityAssembler {
    public static CaseResource toResourceFromEntity(Case caseEntity) {
        Assignation current = caseEntity.getAssignations().stream()
                .filter(assignment -> assignment.getClosedAt() == null)
                .findFirst().orElse(null);
        if (current == null && caseEntity.getStatus() == CaseStatus.CLOSED) {
            current = caseEntity.getAssignations().stream()
                    .max(java.util.Comparator.comparing(Assignation::getAssignedAt)).orElse(null);
        }
        var submission = caseEntity.getConsultationSubmission();
        return new CaseResource(caseEntity.getId(), submission.getId(),
                submission.getCitizenUserId() == null ? null : submission.getCitizenUserId().userId(),
                caseEntity.getStatus().name(),
                current == null ? null : current.getProfessionalUserId().userId(),
                caseEntity.getObservation(), caseEntity.getClosedAt(), caseEntity.getCreatedAt());
    }
}
