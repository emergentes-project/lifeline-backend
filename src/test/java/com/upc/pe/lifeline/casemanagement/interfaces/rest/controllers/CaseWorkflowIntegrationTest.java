package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.ProcessCaseJobCommandService;
import com.upc.pe.lifeline.casemanagement.domain.services.ReceiveConsultationSubmissionCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import com.upc.pe.lifeline.iam.domain.model.aggregates.User;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterCitizenCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.RegisterMedicalProfessionalCommand;
import com.upc.pe.lifeline.iam.domain.model.commands.UpdateMedicalProfileCommand;
import com.upc.pe.lifeline.iam.domain.services.UserCommandService;
import com.upc.pe.lifeline.iam.domain.services.MedicalProfileCommandService;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:case_workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@AutoConfigureMockMvc
class CaseWorkflowIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserCommandService users;

    @Autowired
    private MedicalProfileCommandService medicalProfiles;

    @Autowired
    private ReceiveConsultationSubmissionCommandService reception;

    @Autowired
    private ProcessCaseJobCommandService jobs;

    @Autowired
    private OutboxEventRepository events;

    @Autowired
    private CaseRepository cases;

    @Autowired
    private JwtService jwt;

    @Test
    void caseCanBeReleasedReassignedStartedAndClosedWithHistory() throws Exception {
        User citizen = citizen();
        User professional = professional("A348" + digits() + "23");
        UUID caseId = caseFor(citizen);
        String medicalToken = token(professional, "MEDICAL_PROFESSIONAL");

        mvc.perform(get("/api/v1/cases").header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(caseId.toString()));
        mvc.perform(post("/api/v1/cases/{id}/assign", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
        mvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.assignedProfessionalUserId").doesNotExist());
        mvc.perform(post("/api/v1/cases/{id}/assign", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"AVAILABLE\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/cases/{id}/close", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observation\":\"Atendido\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(get("/api/v1/cases/{id}/history", caseId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/v1/cases/me").header(HttpHeaders.AUTHORIZATION, token(citizen, "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(caseId.toString()));
        mvc.perform(get("/api/v1/cases/paged").header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .param("status", "CLOSED").param("assignmentRelation", "ASSIGNED_TO_ME")
                        .param("province", "Lim").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(caseId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        mvc.perform(get("/api/v1/cases/paged").header(HttpHeaders.AUTHORIZATION, medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        mvc.perform(get("/api/v1/cases/me/paged")
                        .header(HttpHeaders.AUTHORIZATION, token(citizen, "CITIZEN"))
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        UUID consultationId = cases.findById(caseId).orElseThrow().getConsultationSubmission().getId();
        mvc.perform(get("/api/v1/consultation-submissions/{id}", consultationId)
                        .header(HttpHeaders.AUTHORIZATION, medicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterPhone").value("999111222"));
        mvc.perform(get("/api/v1/consultation-submissions")
                        .header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .param("province", "Lima").param("helpRequested", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patient.province").value("Lima"));
    }

    @Test
    void approvedProfessionalCanSeeGuestRequesterPhone() throws Exception {
        User professional = professional("D348" + digits() + "23");
        UUID caseId = caseFor(null, "988777666");
        UUID consultationId = cases.findById(caseId).orElseThrow().getConsultationSubmission().getId();
        mvc.perform(get("/api/v1/consultation-submissions/{id}", consultationId)
                        .header(HttpHeaders.AUTHORIZATION, token(professional, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterPhone").value("988777666"));
    }

    @Test
    void pagedCasesKeepFiltersAcrossPages() throws Exception {
        User professional = professional("E348" + digits() + "23");
        String medicalToken = token(professional, "MEDICAL_PROFESSIONAL");
        UUID first = caseFor(citizen());
        UUID second = caseFor(citizen());
        mvc.perform(post("/api/v1/cases/{id}/assign", first)
                .header(HttpHeaders.AUTHORIZATION, medicalToken)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/cases/{id}/assign", second)
                .header(HttpHeaders.AUTHORIZATION, medicalToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/cases/paged").header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .param("status", "ASSIGNED").param("assignmentRelation", "ASSIGNED_TO_ME")
                        .param("province", "Lima").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/cases/paged").header(HttpHeaders.AUTHORIZATION, medicalToken)
                        .param("status", "ASSIGNED").param("assignmentRelation", "ASSIGNED_TO_ME")
                        .param("province", "Lima").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void unapprovedOrDifferentProfessionalCannotTakeOverAndOtherCitizenCannotRead() throws Exception {
        User owner = citizen();
        User otherCitizen = citizen();
        User approved = professional("B348" + digits() + "23");
        User unapproved = professional("INVALID-LICENSE");
        User otherProfessional = professional("C348" + digits() + "23");
        UUID caseId = caseFor(owner);

        mvc.perform(get("/api/v1/cases")
                        .header(HttpHeaders.AUTHORIZATION, token(unapproved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cases/{id}", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(unapproved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cases/{id}/history", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(unapproved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isForbidden());
        UUID consultationId = cases.findById(caseId).orElseThrow().getConsultationSubmission().getId();
        mvc.perform(get("/api/v1/consultation-submissions/{id}", consultationId)
                        .header(HttpHeaders.AUTHORIZATION, token(unapproved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cases/{id}/assign", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(unapproved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cases/{id}/assign", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(owner, "CITIZEN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cases/{id}/assign", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(approved, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(otherProfessional, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/cases/{id}", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(otherCitizen, "CITIZEN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/consultation-submissions/{id}", consultationId)
                        .header(HttpHeaders.AUTHORIZATION, token(owner, "CITIZEN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/consultation-submissions/{id}", consultationId)
                        .header(HttpHeaders.AUTHORIZATION, token(otherCitizen, "CITIZEN")))
                .andExpect(status().isForbidden());
        assertThat(cases.findById(caseId).orElseThrow().getStatus().name()).isEqualTo("ASSIGNED");

        medicalProfiles.handle(new UpdateMedicalProfileCommand(approved.getId(), "DOCTOR", "INVALID-LICENSE", null));
        mvc.perform(patch("/api/v1/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, token(approved, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newStatus\":\"AVAILABLE\"}"))
                .andExpect(status().isOk());
    }

    private UUID caseFor(User citizen) {
        return caseFor(citizen, null);
    }

    private UUID caseFor(User citizen, String requesterPhone) {
        var receptionResult = reception.handle(new ReceiveConsultationSubmissionCommand(
                UUID.randomUUID(), citizen == null ? null : citizen.getId(), "Necesito ayuda", true,
                new ReceiveConsultationSubmissionCommand.PatientData("Ana", 31, "Centro", "Lima", null, null),
                null, LocalDateTime.now(ZoneOffset.UTC), UUID.randomUUID(), requesterPhone));
        UUID submissionId = receptionResult.submission().getId();
        UUID jobId = events.findAll().stream()
                .filter(event -> event.getAggregateId().equals(submissionId))
                .findFirst().orElseThrow().getId();
        jobs.handle(jobId);
        return cases.findByConsultationSubmission_Id(submissionId).orElseThrow().getId();
    }

    private User citizen() {
        return users.handle(new RegisterCitizenCommand(UUID.randomUUID() + "@example.com", "secret",
                "Ana", "Perez", "999111222", "Lima", "Centro", "Av. 1", LocalDate.of(1995, 1, 1), null, null));
    }

    private User professional(String license) {
        return users.handle(new RegisterMedicalProfessionalCommand(UUID.randomUUID() + "@example.com",
                "secret", "Luis", "Ruiz", null, "DOCTOR", license, null));
    }

    private String token(User user, String role) {
        return "Bearer " + jwt.generateToken(new UserId(user.getId()), role);
    }

    private String digits() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }
}
