package com.upc.pe.lifeline;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Assignation;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.CaseStatusHistory;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.ConsultationAnswer;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Patient;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.iam.domain.model.aggregates.CitizenProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.MedicalProfile;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.entities.Role;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.EmergencyContact;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.ProfessionalCategory;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesCode;
import com.upc.pe.lifeline.iam.domain.model.valueobjects.RolesName;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:backend_entities;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
@Transactional
class BackendEntitiesPersistenceTest {
    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsIamRelationsEnumsAndTheEmbeddedEmergencyContact() {
        Role citizenRole = entityManager.createQuery("select r from Role r where r.code = :code", Role.class)
                .setParameter("code", RolesCode.CIT1).getSingleResult();
        Role medicalRole = entityManager.createQuery("select r from Role r where r.code = :code", Role.class)
                .setParameter("code", RolesCode.MED2).getSingleResult();
        User citizen = new User("citizen@example.test", "password-hash", "Ana", "Perez", null, citizenRole);
        User professional = new User("medical@example.test", "password-hash", "Luis", "Ruiz", null, medicalRole);
        entityManager.persist(citizen);
        entityManager.persist(professional);
        EmergencyContact contact = new EmergencyContact("Maria Perez", "+51987654321");
        CitizenProfile citizenProfile = new CitizenProfile(citizen, "Lima", "Miraflores", "Av. Principal 123",
                LocalDate.of(1995, 1, 20), null, null, contact.name(), contact.phone());
        MedicalProfile medicalProfile = new MedicalProfile(professional, "DOCTOR",
                "CMP-12345", "Emergency medicine");
        entityManager.persist(citizenProfile);
        entityManager.persist(medicalProfile);
        entityManager.flush();
        entityManager.clear();

        CitizenProfile storedCitizen = entityManager.find(CitizenProfile.class, citizenProfile.getId());
        MedicalProfile storedMedical = entityManager.find(MedicalProfile.class, medicalProfile.getId());
        assertThat(storedCitizen.getEmergencyContact()).isEqualTo(contact);
        assertThat(storedCitizen.getUser().getRole().getCode()).isEqualTo(RolesCode.CIT1);
        assertThat(storedMedical.getProfessionalCategory()).isEqualTo(ProfessionalCategory.DOCTOR);
        assertThat(storedMedical.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(storedMedical.getUser().getRole().getName()).isEqualTo(RolesName.MEDICAL_PROFESSIONAL);
    }

    @Test
    void persistsAPhotoOnlySubmissionWithItsPatientAnswerAndCaseHistory() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Patient patient = new Patient(null, "Ana", 30, "Near the main square", "Lima",
                new BigDecimal("-12.046374"), new BigDecimal("-77.042793"));
        UUID patientId = patient.getId();
        assertThat(patientId).isNotNull();
        List<Map<String, Object>> references = List.of(Map.of("source", "baseline", "section", "first-aid"));
        ConsultationAnswer answer = new ConsultationAnswer(null, "Request professional assistance",
                "GENERATED", "HIGH", true, references);
        UUID answerId = answer.getId();
        assertThat(answerId).isNotNull();
        UUID citizenId = UUID.randomUUID();
        UUID mobileConsultationId = UUID.randomUUID();
        ConsultationSubmission submission = new ConsultationSubmission(mobileConsultationId, citizenId, null,
                null, true, now, now, UUID.randomUUID(), patient, answer);
        UUID submissionId = submission.getId();
        assertThat(submissionId).isNotNull();
        submission.setPhotoStorageKey("lifeline/consultations/" + mobileConsultationId);
        entityManager.persist(submission);
        Case caseEntity = new Case(submission);
        UUID professionalId = UUID.randomUUID();
        Assignation assignation = new Assignation(caseEntity, professionalId, now);
        CaseStatusHistory history = new CaseStatusHistory(caseEntity, null, CaseStatus.AVAILABLE,
                "SYSTEM", null, now);
        caseEntity.getAssignations().add(assignation);
        caseEntity.getStatusHistory().add(history);
        entityManager.persist(caseEntity);
        entityManager.flush();
        entityManager.clear();

        ConsultationSubmission storedSubmission = entityManager.find(ConsultationSubmission.class, submissionId);
        Case storedCase = entityManager.find(Case.class, caseEntity.getId());
        assertThat(storedSubmission.getMobileConsultationId()).isEqualTo(mobileConsultationId);
        assertThat(storedSubmission.getInputText()).isNull();
        assertThat(storedSubmission.getCitizenUserId()).isEqualTo(new UserId(citizenId));
        assertThat(storedSubmission.getPatient().getId()).isEqualTo(patientId);
        assertThat(storedSubmission.getAnswer().getId()).isEqualTo(answerId);
        assertThat(storedSubmission.getAnswer().getSourceReferences()).isEqualTo(references);
        assertThat(storedCase.getConsultationSubmission().getId()).isEqualTo(submissionId);
        assertThat(storedCase.getStatus()).isEqualTo(CaseStatus.AVAILABLE);
        assertThat(storedCase.getAssignations()).extracting(Assignation::getProfessionalUserId)
                .containsExactly(new UserId(professionalId));
        assertThat(storedCase.getStatusHistory()).extracting(CaseStatusHistory::getToStatus)
                .containsExactly(CaseStatus.AVAILABLE);
        assertThat(storedCase.getStatusHistory()).extracting(CaseStatusHistory::getUserId)
                .containsExactly((UserId) null);

        storedSubmission.setCitizenUserId(null);
        storedCase.getStatusHistory().getFirst().setUserId(new UserId(professionalId));
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(ConsultationSubmission.class, submissionId).getCitizenUserId()).isNull();
        assertThat(entityManager.find(CaseStatusHistory.class, history.getId()).getUserId())
                .isEqualTo(new UserId(professionalId));
    }

}
