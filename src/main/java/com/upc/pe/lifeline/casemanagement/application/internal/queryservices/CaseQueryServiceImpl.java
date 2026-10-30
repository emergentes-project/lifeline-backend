package com.upc.pe.lifeline.casemanagement.application.internal.queryservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseByIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCasesByFilterQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCaseStatusHistoryByCaseIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetMyCasesQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetCasePageQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.PreliminaryPriority;
import com.upc.pe.lifeline.casemanagement.domain.services.CaseQueryService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CaseQueryServiceImpl implements CaseQueryService {
    private final CaseRepository cases;
    private final CaseStatusHistoryRepository history;

    public CaseQueryServiceImpl(CaseRepository cases, CaseStatusHistoryRepository history) {
        this.cases = cases;
        this.history = history;
    }

    @Override
    @Transactional(readOnly = true)
    public Case handle(GetCaseByIdQuery query) {
        return cases.findById(query.caseId()).orElseThrow(() -> new NoSuchElementException("Case not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Case> handle(GetCasesByFilterQuery query) {
        CaseStatus status = query.status() == null
                ? null : CaseStatus.valueOf(query.status().strip().toUpperCase(java.util.Locale.ROOT));
        String assignmentRelation = query.assignmentRelation() == null
                ? null : query.assignmentRelation().strip().toUpperCase(java.util.Locale.ROOT);
        if (assignmentRelation != null && !List.of("ASSIGNED_TO_ME", "NOT_ASSIGNED_TO_ME")
                .contains(assignmentRelation)) {
            throw new IllegalArgumentException("Invalid assignment relation");
        }
        return cases.findAllByOrderByCreatedAtDesc().stream()
                .filter(caseEntity -> status == null || caseEntity.getStatus() == status)
                .filter(caseEntity -> matchesAssignment(caseEntity, assignmentRelation,
                        query.currentProfessionalUserId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Case> handle(GetMyCasesQuery query) {
        return cases.findAllForCitizen(query.currentCitizenUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Case> handle(GetCasePageQuery query) {
        CaseStatus status = query.status() == null || query.status().isBlank() ? null
                : CaseStatus.valueOf(query.status().strip().toUpperCase(java.util.Locale.ROOT));
        PreliminaryPriority priority = query.priority() == null || query.priority().isBlank() ? null
                : PreliminaryPriority.valueOf(query.priority().strip().toUpperCase(java.util.Locale.ROOT));
        String relation = query.assignmentRelation() == null || query.assignmentRelation().isBlank() ? null
                : query.assignmentRelation().strip().toUpperCase(java.util.Locale.ROOT);
        if (relation != null && !List.of("ASSIGNED_TO_ME", "NOT_ASSIGNED_TO_ME").contains(relation)) {
            throw new IllegalArgumentException("Invalid assignment relation");
        }
        String province = query.province() == null || query.province().isBlank() ? null : query.province().strip();
        var pageable = PageRequest.of(query.page(), query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var ids = cases.findPageIds(query.citizenUserId(), query.professionalUserId(),
                status, priority, province, relation, pageable);
        if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
        Map<UUID, Case> byId = cases.findByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Case::getId, Function.identity()));
        return new PageImpl<>(ids.getContent().stream().map(byId::get).toList(),
                pageable, ids.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseStatusHistory> handle(GetCaseStatusHistoryByCaseIdQuery query) {
        if (!cases.existsById(query.caseId())) {
            throw new NoSuchElementException("Case not found");
        }
        return history.findAllByCaseEntity_IdOrderByOccurredAtAsc(query.caseId());
    }

    private boolean matchesAssignment(Case caseEntity, String relation,
                                      UUID professionalUserId) {
        if (relation == null) {
            return true;
        }
        boolean assignedToMe = caseEntity.getAssignations().stream()
                .anyMatch(assignment -> assignment.getClosedAt() == null
                        && assignment.getProfessionalUserId().userId().equals(professionalUserId));
        return "ASSIGNED_TO_ME".equals(relation)
                ? assignedToMe : !assignedToMe;
    }
}
