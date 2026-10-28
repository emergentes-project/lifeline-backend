package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:case_jobs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
@AutoConfigureMockMvc
class CaseJobControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @Autowired
    private OutboxEventRepository events;

    @Autowired
    private CaseRepository cases;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private CaseRepository caseSpy;

    @Test
    void anotherServiceSubjectIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/internal/case-jobs/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken("other-service")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void aJobIdThatIsNotAUuidIsRejected() throws Exception {
        mvc.perform(post("/api/v1/internal/case-jobs/not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void aMissingJobIsNotFound() throws Exception {
        mvc.perform(post("/api/v1/internal/case-jobs/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void aJobWhoseConsultationIsMissingIsNotFound() throws Exception {
        OutboxEvent event = events.saveAndFlush(new OutboxEvent(UUID.randomUUID(), "consultation.received", Map.of()));

        mvc.perform(post("/api/v1/internal/case-jobs/" + event.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createsOneAvailableCaseAndTreatsARepeatedJobOrConsultationAsAlreadyPresent() throws Exception {
        ConsultationSubmission submission = submissions.saveAndFlush(submission());
        OutboxEvent first = events.saveAndFlush(new OutboxEvent(submission.getId(), "consultation.received", Map.of()));
        OutboxEvent second = events.saveAndFlush(new OutboxEvent(submission.getId(), "consultation.received", Map.of()));

        mvc.perform(post("/api/v1/internal/case-jobs/" + first.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        Case created = cases.findByConsultationSubmission_Id(submission.getId()).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(CaseStatus.AVAILABLE);

        mvc.perform(post("/api/v1/internal/case-jobs/" + first.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mvc.perform(post("/api/v1/internal/case-jobs/" + second.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertThat(cases.findByConsultationSubmission_Id(submission.getId()).orElseThrow().getId()).isEqualTo(created.getId());
    }

    @Test
    void aUniqueConsultationRaceReturnsTheExistingCase() throws Exception {
        ConsultationSubmission submission = submissions.saveAndFlush(submission());
        OutboxEvent event = events.saveAndFlush(new OutboxEvent(submission.getId(), "consultation.received", Map.of()));
        doAnswer(invocation -> {
            Case incoming = invocation.getArgument(0);
            UUID consultationId = incoming.getConsultationSubmission().getId();
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status -> {
                ConsultationSubmission stored = entityManager.find(ConsultationSubmission.class, consultationId);
                entityManager.persist(new Case(stored));
                entityManager.flush();
            });
            throw new DataIntegrityViolationException("duplicate consultation");
        }).when(caseSpy).saveAndFlush(any(Case.class));

        mvc.perform(post("/api/v1/internal/case-jobs/" + event.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertThat(cases.findByConsultationSubmission_Id(submission.getId())).isPresent();
    }

    private String bearer() {
        return "Bearer " + jwtService.generateServiceToken("lifeline-worker");
    }

    private ConsultationSubmission submission() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new ConsultationSubmission(UUID.randomUUID(), null, "dolor", null, true, now, now,
                UUID.randomUUID(), null, null);
    }
}
