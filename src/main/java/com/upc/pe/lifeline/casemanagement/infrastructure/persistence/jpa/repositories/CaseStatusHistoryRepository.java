package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseStatusHistoryRepository extends JpaRepository<CaseStatusHistory, UUID> {
    List<CaseStatusHistory> findAllByCaseEntity_IdOrderByOccurredAtAsc(UUID caseId);
}
