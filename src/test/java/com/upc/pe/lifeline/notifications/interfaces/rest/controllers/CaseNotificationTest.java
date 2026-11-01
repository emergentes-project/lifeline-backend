package com.upc.pe.lifeline.notifications.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.application.internal.eventhandlers.CaseStatusNotificationListener;
import com.upc.pe.lifeline.casemanagement.application.ports.ProfessionalEligibilityChecker;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.Case;
import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.notifications.application.internal.commandservices.NotificationDeliveryService;
import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.model.NotificationSendResult;
import com.upc.pe.lifeline.notifications.domain.services.CaseNotificationSender;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationDeliveryRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "lifeline.notifications.firebase.enabled=false",
        "lifeline.notifications.delivery.scheduler-enabled=false"
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
    private NotificationDeliveryRepository deliveries;

    @Autowired
    private NotificationDeliveryService deliveryService;

    @Autowired
    private CaseStatusNotificationListener listener;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CaseNotificationSender sender;

    @MockitoBean
    private ProfessionalEligibilityChecker eligibility;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(eligibility.isApproved(any(UUID.class))).thenReturn(true);
        when(sender.send(any(), any())).thenReturn(new NotificationSendResult(true, List.of()));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        deliveries.deleteAll();
        cases.deleteAll();
        submissions.deleteAll();
        tokens.deleteAll();
    }

    @Test
    void theOwnerReceivesEachStatusOnceAndAnotherCitizenDoesNot() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherCitizenId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        ConsultationSubmission consultation = consultation(ownerId, true);
        Case caseEntity = cases.save(new Case(consultation));
        register(ownerId, "device-owner", consultation.getId());
        register(otherCitizenId, "device-other", null);

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        deliveryService.dispatchDue();
        deliveryService.dispatchDue();
        verify(sender, times(1)).send(message("ASSIGNED", "Un profesional asumió tu caso",
                "Tu solicitud ya tiene un profesional asignado.", caseEntity.getId()), eq(List.of("device-owner")));

        mvc.perform(patch("/api/v1/cases/" + caseEntity.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        deliveryService.dispatchDue();
        deliveryService.dispatchDue();
        verify(sender, times(1)).send(message("IN_PROGRESS", "Tu caso está en atención",
                "Un profesional inició la atención.", caseEntity.getId()), eq(List.of("device-owner")));

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/close")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observation\":\"alta\"}"))
                .andExpect(status().isOk());
        deliveryService.dispatchDue();
        verify(sender).send(message("CLOSED", "Tu caso fue cerrado",
                "La atención del caso terminó.", caseEntity.getId()), eq(List.of("device-owner")));
        verify(sender, never()).send(any(), eq(List.of("device-other")));
    }

    @Test
    void aGuestRegistrationDoesNotReplaceAnOwnedToken() throws Exception {
        UUID ownerId = UUID.randomUUID();
        register(ownerId, "shared-phone", null);
        ConsultationSubmission guestConsultation = consultation(null, true);

        mvc.perform(post("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"shared-phone\",\"consultationId\":\"" + guestConsultation.getId() + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(tokens.findByToken("shared-phone").orElseThrow().getUserId()).isEqualTo(ownerId);
        assertThat(tokens.findByToken("shared-phone").orElseThrow().getConsultationId()).isNull();
    }

    @Test
    void aDeliveryFailureStaysPendingAndKeepsTheCase() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        ConsultationSubmission consultation = consultation(ownerId, true);
        Case caseEntity = cases.save(new Case(consultation));
        register(ownerId, "device-fails", null);
        doThrow(new RuntimeException("push failed")).when(sender).send(any(), any());

        mvc.perform(post("/api/v1/cases/" + caseEntity.getId() + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, token(professionalId, "MEDICAL_PROFESSIONAL")))
                .andExpect(status().isOk());
        deliveryService.dispatchDue();

        var delivery = deliveries.findByCaseIdAndStatus(caseEntity.getId(), "ASSIGNED").orElseThrow();
        assertThat(delivery.getSentAt()).isNull();
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(cases.findById(caseEntity.getId()).orElseThrow().getStatus()).isEqualTo(CaseStatus.ASSIGNED);
    }

    @Test
    void anInvalidTokenIsRemovedAndARepeatedStatusIsNotQueued() {
        UUID ownerId = UUID.randomUUID();
        ConsultationSubmission consultation = consultation(ownerId, true);
        Case caseEntity = cases.save(new Case(consultation));
        tokens.save(new com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationToken(
                "device-stale", ownerId, null, LocalDateTime.now(ZoneOffset.UTC)));
        when(sender.send(any(), any())).thenReturn(new NotificationSendResult(true, List.of("device-stale")));

        listener.on(new CaseStatusChangedEvent(caseEntity.getId(), CaseStatus.AVAILABLE, CaseStatus.ASSIGNED));
        listener.on(new CaseStatusChangedEvent(caseEntity.getId(), CaseStatus.ASSIGNED, CaseStatus.ASSIGNED));
        listener.on(new CaseStatusChangedEvent(UUID.randomUUID(), CaseStatus.ASSIGNED, CaseStatus.AVAILABLE));
        assertThat(deliveries.findAll()).hasSize(1);

        deliveryService.dispatchDue();
        assertThat(tokens.findByToken("device-stale")).isEmpty();
        assertThat(deliveries.findByCaseIdAndStatus(caseEntity.getId(), "ASSIGNED").orElseThrow().getSentAt())
                .isNotNull();
    }

    @Test
    void deleteRemovesOnlyTheAuthenticatedCitizensToken() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        register(ownerId, "device-owner", null);
        register(otherId, "device-other", null);

        mvc.perform(delete("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(otherId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-owner\"}"))
                .andExpect(status().isForbidden());
        assertThat(tokens.findByToken("device-owner")).isPresent();

        mvc.perform(delete("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(ownerId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-owner\"}"))
                .andExpect(status().isNoContent());
        assertThat(tokens.findByToken("device-owner")).isEmpty();
        assertThat(tokens.findByToken("device-other")).isPresent();

        mvc.perform(delete("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(ownerId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-owner\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(UUID.randomUUID(), "MEDICAL_PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-other\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/notification-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"device-other\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(tokens.findByToken("device-other")).isPresent();
    }

    @Test
    void aCitizenRegistrationReplacesTheOwnerWithoutKeepingTheConsultation() throws Exception {
        UUID firstCitizen = UUID.randomUUID();
        UUID secondCitizen = UUID.randomUUID();
        register(firstCitizen, "same-device", consultation(firstCitizen, true).getId());
        register(secondCitizen, "same-device", consultation(secondCitizen, true).getId());

        assertThat(tokens.findAll()).hasSize(1);
        assertThat(tokens.findByToken("same-device").orElseThrow().getUserId()).isEqualTo(secondCitizen);
        assertThat(tokens.findByToken("same-device").orElseThrow().getConsultationId()).isNull();
    }

    private void register(UUID citizenId, String deviceToken, UUID consultationId) throws Exception {
        String consultationJson = consultationId == null ? "null" : "\"" + consultationId + "\"";
        mvc.perform(post("/api/v1/notification-tokens")
                        .header(HttpHeaders.AUTHORIZATION, token(citizenId, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + deviceToken + "\",\"consultationId\":" + consultationJson + "}"))
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
