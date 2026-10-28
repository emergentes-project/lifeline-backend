package com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationSubmissionRepository extends JpaRepository<ConsultationSubmission, UUID> {
    Optional<ConsultationSubmission> findByIdempotencyKey(UUID idempotencyKey);
}
