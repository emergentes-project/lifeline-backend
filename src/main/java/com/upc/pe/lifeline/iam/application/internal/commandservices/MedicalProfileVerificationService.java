package com.upc.pe.lifeline.iam.application.internal.commandservices;

import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MedicalProfileVerificationService {
    private static final Pattern LICENSE_FORMAT = Pattern.compile("[A-Z][0-9]{3}[0-9]{8}[0-9]{2}");

    private final MedicalProfileRepository medicalProfileRepository;

    public MedicalProfileVerificationService(MedicalProfileRepository medicalProfileRepository) {
        this.medicalProfileRepository = medicalProfileRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verify(UUID userId) {
        MedicalProfile profile = medicalProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NoSuchElementException("Medical profile not found for user " + userId));
        if (!"PENDING".equals(profile.getVerificationStatus())) {
            return;
        }
        String licenseNumber = profile.getProfessionalLicenseNumber();
        boolean hasExpectedFormat = LICENSE_FORMAT.matcher(licenseNumber).matches();
        boolean isAlreadyRegistered = medicalProfileRepository
                .existsByProfessionalLicenseNumberIgnoreCaseAndIdNot(licenseNumber, profile.getId());
        if (hasExpectedFormat && !isAlreadyRegistered) {
            profile.approveVerification();
        } else {
            profile.rejectVerification();
        }
        medicalProfileRepository.save(profile);
    }
}
