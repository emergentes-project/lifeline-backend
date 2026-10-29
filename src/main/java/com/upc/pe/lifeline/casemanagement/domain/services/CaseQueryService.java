package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseByIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCasesByFilterQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseStatusHistoryByCaseIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetMyCasesQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCasePageQuery;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CaseQueryService {
    Case handle(GetCaseByIdQuery query);
    List<Case> handle(GetCasesByFilterQuery query);
    List<Case> handle(GetMyCasesQuery query);
    Page<Case> handle(GetCasePageQuery query);
    List<CaseStatusHistory> handle(GetCaseStatusHistoryByCaseIdQuery query);
}
