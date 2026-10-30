package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.FilterConsultationSubmissionsQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationSubmissionByIdQuery;

import java.util.List;

public interface ConsultationSubmissionQueryService {
    ConsultationSubmission handle(GetConsultationSubmissionByIdQuery query);
    List<ConsultationSubmission> handle(FilterConsultationSubmissionsQuery query);
}
