package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.jayway.jsonpath.JsonPath;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultation_http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class ConsultationSubmissionControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @Autowired
    private OutboxEventRepository events;

    @Test
    void receptionStoresRequesterPhoneForGuest() throws Exception {
        UUID key = UUID.randomUUID();
        String body = """
                {"mobileConsultationId":"%s","inputText":"Ayuda","helpRequested":true,
                 "requesterPhone":"988777666","consultationRecordedAt":"2026-10-27T18:00:00",
                 "idempotencyKey":"%s"}
                """.formatted(UUID.randomUUID(), key);
        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        assertThat(submissions.findByIdempotencyKey(key).orElseThrow().getRequesterPhone())
                .isEqualTo("988777666");
    }

    @Test
    void firstReceptionReturnsCreatedAndTheReplayReturnsTheStoredSubmission() throws Exception {
        UUID key = UUID.randomUUID();
        UUID mobileId = UUID.randomUUID();
        String body = body(mobileId, key, true, "dolor de pecho");

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key.toString())
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.idempotencyKey").value(key.toString()))
                .andExpect(jsonPath("$.helpRequested").value(true));

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID(), key, false, "otro texto")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value(key.toString()))
                .andExpect(jsonPath("$.helpRequested").value(true));
    }

    @Test
    void aDifferentIdempotencyHeaderIsRejected() throws Exception {
        String body = body(UUID.randomUUID(), UUID.randomUUID(), true, "texto");

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void aSubmissionWithoutTextWaitsForPhotoBeforeCreatingAnEvent() throws Exception {
        UUID key = UUID.randomUUID();
        String body = """
                {"mobileConsultationId":"%s","helpRequested":true,"consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s"}
                """.formatted(UUID.randomUUID(), key);

        String created = mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID consultationId = UUID.fromString(JsonPath.read(created, "$.id"));

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(consultationId.toString()));

        var pending = events.findAll().stream()
                .filter(event -> consultationId.equals(event.getAggregateId()))
                .toList();
        assertThat(pending).isEmpty();
    }

    @Test
    @Transactional
    void patientPhoneIsStoredWithoutSurroundingSpaces() throws Exception {
        UUID key = UUID.randomUUID();
        String body = """
                {"mobileConsultationId":"%s","inputText":"dolor","helpRequested":false,"consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s","patient":{"name":"Ana","phone":"  +51987654321  "}}
                """.formatted(UUID.randomUUID(), key);

        String created = mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID consultationId = UUID.fromString(JsonPath.read(created, "$.id"));

        var stored = submissions.findById(consultationId).orElseThrow();
        assertThat(stored.getPatient().getPhone()).isEqualTo("+51987654321");
        assertThat(stored.getRequesterPhone()).isEqualTo("+51987654321");
    }

    @Test
    void aPatientPhoneLongerThanThirtyCharactersIsRejected() throws Exception {
        String body = """
                {"mobileConsultationId":"%s","inputText":"dolor","helpRequested":true,"consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s","patient":{"phone":" %s "}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), "1".repeat(31));

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theSameMobileConsultationWithAnotherKeyConflicts() throws Exception {
        UUID mobileId = UUID.randomUUID();
        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(mobileId, UUID.randomUUID(), false, "primera")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(mobileId, UUID.randomUUID(), false, "segunda")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private static String body(UUID mobileId, UUID idempotencyKey, boolean helpRequested, String inputText) {
        return """
                {"mobileConsultationId":"%s","inputText":"%s","helpRequested":%s,"consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s"}
                """.formatted(mobileId, inputText, helpRequested, idempotencyKey);
    }
}
