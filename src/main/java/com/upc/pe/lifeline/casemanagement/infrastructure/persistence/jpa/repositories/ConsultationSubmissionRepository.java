package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ConsultationSubmissionRepository extends JpaRepository<ConsultationSubmission, UUID> {
    Optional<ConsultationSubmission> findByIdempotencyKey(UUID idempotencyKey);

    @EntityGraph(attributePaths = {"patient", "answer"})
    Optional<ConsultationSubmission> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ConsultationSubmission s where s.id = :id")
    Optional<ConsultationSubmission> findByIdForUpdate(UUID id);

    @EntityGraph(attributePaths = {"patient", "answer"})
    List<ConsultationSubmission> findAllByOrderByReceivedAtDesc();
}
