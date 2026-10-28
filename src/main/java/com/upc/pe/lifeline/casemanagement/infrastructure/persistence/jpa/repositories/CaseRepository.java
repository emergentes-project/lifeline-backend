package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<Case, UUID> {
    Optional<Case> findByConsultationSubmission_Id(UUID consultationId);

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    Optional<Case> findById(UUID id);

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    List<Case> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"consultationSubmission", "assignations"})
    @Query("select c from Case c where c.consultationSubmission.citizenUserId.userId = :userId order by c.createdAt desc")
    List<Case> findAllForCitizen(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Case c where c.id = :id")
    Optional<Case> findByIdForUpdate(UUID id);
}
