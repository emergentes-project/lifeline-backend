package com.upc.pe.lifeline.notifications.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.application.internal.eventhandlers.CaseStatusNotificationListener;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.services.CaseNotificationSender;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationTokenRepository;
import com.upc.pe.lifeline.shared.domain.model.valueobjects.UserId;
import com.upc.pe.lifeline.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:case_notifications;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false",
        "lifeline.notifications.firebase.enabled=false"
})
class CaseNotificationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @Autowired
    private CaseRepository cases;

    @Autowired
    private NotificationTokenRepository tokens;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CaseStatusNotificationListener listener;

    @MockitoBean
    private CaseNotificationSender sender;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        cases.deleteAll();
        submissions.deleteAll();
        tokens.deleteAll();
    }

    @Test
    void assignedAndInProgressEachSendOnceAndARepeatedAssignDoesNot() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        ConsultationSubmission consultation = consultation(citizenId, true);
        Case caseEntity = cases.save(new Case(consultation));
        register(citizenId, "device-citizen", consultation.getId());

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        verify(sender).send(message("ASSIGNED", "Un profesional asumió tu caso",
                "Tu solicitud ya tiene un profesional asignado.", caseEntity.getId()), eq(List.of("device-citizen")));

        mvc.perform(patch("/api/v1/cases/" + caseEntity.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        verify(sender).send(message("IN_PROGRESS", "Tu caso está en atención",
                "Un profesional inició la atención.", caseEntity.getId()), eq(List.of("device-citizen")));

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isConflict());
        verify(sender, never()).send(message("CLOSED", "Tu caso está en atención",
                "Un profesional inició la atención.", caseEntity.getId()), any());
        org.mockito.Mockito.verifyNoMoreInteractions(sender);
    }

    @Test
    void closedAndRepeatedStatusDoNotSend() {
        listener.on(new CaseStatusChangedEvent(UUID.randomUUID(), CaseStatus.ASSIGNED, CaseStatus.CLOSED));
        listener.on(new CaseStatusChangedEvent(UUID.randomUUID(), CaseStatus.ASSIGNED, CaseStatus.ASSIGNED));
        listener.on(new CaseStatusChangedEvent(UUID.randomUUID(), CaseStatus.AVAILABLE, CaseStatus.AVAILABLE));
        verify(sender, never()).send(any(), any());
    }

    @Test
    void aSenderFailureLeavesTheCaseAssigned() throws Exception {
        UUID citizenId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        ConsultationSubmission consultation = consultation(citizenId, true);
        Case caseEntity = cases.save(new Case(consultation));
        register(citizenId, "device-fails", consultation.getId());
        doThrow(new RuntimeException("push failed")).when(sender).send(any(), any());

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());

        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ASSIGNED);
    }

    @Test
    void aGuestTokenReceivesTheAssignmentAndACaseWithoutTokenDoesNot() throws Exception {
        UUID professionalId = UUID.randomUUID();
        ConsultationSubmission guestConsultation = consultation(null, true);
        Case guestCase = cases.save(new Case(guestConsultation));
        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-guest\",\"consultationId\":\"" + guestConsultation.getId() + "\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/cases/" + guestCase.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        verify(sender).send(message("ASSIGNED", "Un profesional asumió tu caso",
                "Tu solicitud ya tiene un profesional asignado.", guestCase.getId()), eq(List.of("device-guest")));

        Case silent = cases.save(new Case(consultation(UUID.randomUUID(), true)));
        mvc.perform(post("/api/v1/cases/" + silent.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        verify(sender, never()).send(message("ASSIGNED", "Un profesional asumió tu caso",
                "Tu solicitud ya tiene un profesional asignado.", silent.getId()), any());
        assertThat(cases.findById(silent.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ASSIGNED);
    }

    @Test
    void tokenRegistrationReplacesTheSameTokenAndRejectsAnInvalidOne() throws Exception {
        UUID firstCitizen = UUID.randomUUID();
        UUID secondCitizen = UUID.randomUUID();
        ConsultationSubmission first = consultation(firstCitizen, true);
        ConsultationSubmission second = consultation(secondCitizen, true);
        register(firstCitizen, "same-device", first.getId());
        register(secondCitizen, "same-device", second.getId());

        assertThat(tokens.findAll()).hasSize(1);
        assertThat(tokens.findByToken("same-device").orElseThrow().getUserId()).isEqualTo(secondCitizen);
        assertThat(tokens.findByToken("same-device").orElseThrow().getConsultationId()).isEqualTo(second.getId());

        mvc.perform(post("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(firstCitizen, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"   \",\"consultationId\":null}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + "x".repeat(256) + "\",\"consultationId\":\"" + first.getId() + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"guest-without-consultation\",\"consultationId\":null}"))
                .andExpect(status().isBadRequest());
        assertThat(tokens.findByToken("x".repeat(256))).isEmpty();
        assertThat(tokens.findByToken("guest-without-consultation")).isEmpty();
    }

    @Test
    void aGuestCannotRegisterAnotherPersonsConsultation() throws Exception {
        UUID citizenId = UUID.randomUUID();
        ConsultationSubmission owned = consultation(citizenId, true);
        ConsultationSubmission withoutHelp = consultation(null, false);

        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"stolen\",\"consultationId\":\"" + owned.getId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"no-help\",\"consultationId\":\"" + withoutHelp.getId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(citizenId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"foreign\",\"consultationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"professional\",\"consultationId\":null}"))
                .andExpect(status().isForbidden());
        assertThat(tokens.findByToken("stolen")).isEmpty();
    }

    private void register(UUID citizenId, String deviceToken, UUID consultationId) throws Exception {
        mvc.perform(post("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(citizenId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + deviceToken + "\",\"consultationId\":\"" + consultationId + "\"}"))
                .andExpect(status().isNoContent());
    }

    private CasePushMessage message(String status, String title, String body, UUID caseId) {
        return argThat(candidate -> candidate != null
                && status.equals(candidate.status())
                && title.equals(candidate.title())
                && body.equals(candidate.body())
                && caseId.equals(candidate.caseId()));
    }

    private ConsultationSubmission consultation(UUID citizenId, boolean helpRequested) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return submissions.save(new ConsultationSubmission(
                UUID.randomUUID(), citizenId, "dolor", null, helpRequested, now, now, UUID.randomUUID(), null, null));
    }

    private String token(UUID userId, String role) {
        return "Bearer " + jwtService.generateToken(new UserId(userId), role);
    }
}
