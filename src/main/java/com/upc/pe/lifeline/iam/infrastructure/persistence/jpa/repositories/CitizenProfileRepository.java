package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<CitizenProfile> findByUser_Id(UUID userId);
}
