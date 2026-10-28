package com.upc.pe.lifeline.iam.application.internal.eventhandlers;

import com.upc.pe.lifeline.iam.application.internal.commandservices.MedicalProfileVerificationService;
import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.infrastructure.persistence.jpa.repositories.MedicalProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medical_verification;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@ActiveProfiles("medical-verification-test")
class MedicalProfileVerificationListenerTest {
    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private MedicalProfileRepository medicalProfileRepository;

    @Autowired
    private MedicalProfileVerificationService verificationService;

    @Test
    void approvesAUniqueLicenseWithTheRequiredStructureAfterRegistration() {
        var user = register("a3480045365123");
        MedicalProfile profile = medicalProfileRepository.findByUser_Id(user.getId()).orElseThrow();

        assertThat(profile.getProfessionalLicenseNumber()).isEqualTo("A3480045365123");
        assertThat(profile.getVerificationStatus()).isEqualTo("APPROVED");
        assertThat(profile.getVerifiedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateAndMalformedLicensesWithoutRejectingRegistration() {
        var first = register("B3480045365123");
        var duplicate = register("B3480045365123");
        var malformed = register("CMP-TEST-001");
        verificationService.verify(first.getId());

        assertThat(medicalProfileRepository.findByUser_Id(first.getId()).orElseThrow().getVerificationStatus())
                .isEqualTo("APPROVED");
        assertThat(medicalProfileRepository.findByUser_Id(duplicate.getId()).orElseThrow().getVerificationStatus())
                .isEqualTo("REJECTED");
        assertThat(medicalProfileRepository.findByUser_Id(malformed.getId()).orElseThrow().getVerificationStatus())
                .isEqualTo("REJECTED");
    }

    private User register(String licenseNumber) {
        return userCommandService.handle(new RegisterMedicalProfessionalCommand(
                UUID.randomUUID() + "@example.com", "ClaveSegura123!", "Luis", "Ruiz", null,
                "DOCTOR", licenseNumber, null));
    }
}
