package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface MedicalProfileRepository extends JpaRepository<MedicalProfile, UUID> {
    Optional<MedicalProfile> findByUser_Id(UUID userId);
    boolean existsByProfessionalLicenseNumberIgnoreCaseAndIdNot(String professionalLicenseNumber, UUID id);
}
