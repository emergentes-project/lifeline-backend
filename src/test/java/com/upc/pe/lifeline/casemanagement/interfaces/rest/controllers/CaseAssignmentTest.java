package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:case_assignment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@Transactional
class CaseAssignmentTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @Autowired
    private CaseRepository cases;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void theAssignedProfessionalCanTakeTheCaseAndASecondAssignDoesNotChangeIt() throws Exception {
        UUID professionalId = UUID.randomUUID();
        Case caseEntity = availableCase();

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.consultationId").value(caseEntity.getConsultationSubmission().getId().toString()))
                .andExpect(jsonPath("$.assignedProfessionalUserId").value(professionalId.toString()));

        entityManager.flush();
        entityManager.clear();
        Case assigned = cases.findById(caseEntity.getId()).orElseThrow();
        assertThat(assigned.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
        assertThat(assigned.getAssignations()).hasSize(1);
        assertThat(assigned.getAssignations().getFirst().getProfessionalUserId()).isEqualTo(new UserId(professionalId));
        assertThat(assigned.getStatusHistory()).extracting(history -> history.getToStatus())
                .containsExactly(CaseStatus.ASSIGNED);

        mvc.perform(patch("/api/v1/cases/" + caseEntity.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedProfessionalUserId").value(professionalId.toString()));

        entityManager.flush();
        entityManager.clear();
        Case inProgress = cases.findById(caseEntity.getId()).orElseThrow();
        assertThat(inProgress.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        assertThat(inProgress.getAssignations()).hasSize(1);

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        entityManager.flush();
        entityManager.clear();
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getAssignations()).hasSize(1);
    }

    @Test
    void anotherProfessionalCannotStartCare() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Case caseEntity = availableCase();
        String owner = token(ownerId, "MEDICAL_PROFESSIONAL");

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/cases/" + caseEntity.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, token(otherId, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        entityManager.flush();
        entityManager.clear();
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ASSIGNED);
    }

    @Test
    void aCitizenCannotAssign() throws Exception {
        Case caseEntity = availableCase();

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "CITIZEN")))
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.AVAILABLE);
    }

    @Test
    void aMissingCaseReturnsNotFound() throws Exception {
        mvc.perform(post("/api/v1/cases/" + UUID.randomUUID() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aStatusOtherThanInProgressIsRejected() throws Exception {
        UUID professionalId = UUID.randomUUID();
        Case caseEntity = availableCase();
        String token = token(professionalId, "MEDICAL_PROFESSIONAL");
        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/cases/" + caseEntity.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        entityManager.flush();
        entityManager.clear();
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ASSIGNED);
    }

    private Case availableCase() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ConsultationSubmission submission = submissions.save(new ConsultationSubmission(
                UUID.randomUUID(), null, "dolor", null, true, now, now, UUID.randomUUID(), null, null));
        return cases.save(new Case(submission));
    }

    private String token(UUID userId, String role) {
        return "Bearer " + jwtService.generateToken(new UserId(userId), role);
    }
}
