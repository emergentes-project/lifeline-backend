package com.upc.pe.lifeline.casemanagement.interfaces.rest.transform;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.CaseStatusHistoryResource;

public class CaseStatusHistoryResourceFromEntityAssembler {
    public static CaseStatusHistoryResource toResourceFromEntity(CaseStatusHistory history) {
        return new CaseStatusHistoryResource(history.getId(), history.getCaseEntity().getId(),
                history.getFromStatus() == null ? null : history.getFromStatus().name(),
                history.getToStatus().name(), history.getActorType(),
                history.getUserId() == null ? null : history.getUserId().userId(), history.getOccurredAt());
    }
}
