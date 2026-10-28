package com.upc.pe.lifeline.iam.infrastructure.persistence.jpa;

import com.upc.pe.lifeline.casemanagement.application.ports.ProfessionalEligibilityChecker;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JpaProfessionalEligibilityChecker implements ProfessionalEligibilityChecker {
    private final MedicalProfileRepository medicalProfiles;

    public JpaProfessionalEligibilityChecker(MedicalProfileRepository medicalProfiles) {
        this.medicalProfiles = medicalProfiles;
    }

    @Override
    public boolean isApproved(UUID professionalUserId) {
        return medicalProfiles.findByUser_Id(professionalUserId)
                .map(profile -> "APPROVED".equals(profile.getVerificationStatus()))
                .orElse(false);
    }
}
