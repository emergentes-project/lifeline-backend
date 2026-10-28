package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultation_photo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false"
})
@AutoConfigureMockMvc
class ConsultationPhotoEndpointTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OutboxEventRepository events;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @MockitoBean
    private ImageStorageService images;

    @Test
    void guestPhotoRequiresTheOriginalKeyAndCreatesOneJobAfterUpload() throws Exception {
        UUID key = UUID.randomUUID();
        String id = createPendingPhotoConsultation(key);
        UUID consultationId = UUID.fromString(id);
        String storageKey = "lifeline/consultations/" + id;
        assertThat(eventsFor(consultationId)).isEmpty();

        mvc.perform(multipart("/api/v1/consultation-submissions/{id}/photo", id)
                        .file(photo()).header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());

        when(images.belongsToConsultation(storageKey, consultationId)).thenReturn(false, true);
        when(images.upload(any(byte[].class), any(UUID.class))).thenReturn(storageKey);
        mvc.perform(multipart("/api/v1/consultation-submissions/{id}/photo", id)
                        .file(photo()).header("Idempotency-Key", key.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageKey").value(storageKey));
        assertThat(eventsFor(consultationId)).hasSize(1);
        assertThat(eventsFor(consultationId).getFirst().getStatus()).isEqualTo("PENDING");
        assertThat(submissions.findById(consultationId).orElseThrow().getPhotoStorageKey()).isEqualTo(storageKey);

        mvc.perform(multipart("/api/v1/consultation-submissions/{id}/photo", id)
                        .file(photo()).header("Idempotency-Key", key.toString()))
                .andExpect(status().isOk());
        assertThat(eventsFor(consultationId)).hasSize(1);

        when(images.getDownloadUrl(storageKey)).thenReturn("https://example.com/signed-photo");
        mvc.perform(get("/api/v1/consultation-submissions/{id}/photo", id)
                        .header("Idempotency-Key", key.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/signed-photo"));
    }

    @Test
    void aTextConsultationKeepsItsOriginalJobWhenPhotoArrivesLater() throws Exception {
        UUID key = UUID.randomUUID();
        UUID mobileId = UUID.randomUUID();
        String body = """
                {"mobileConsultationId":"%s","inputText":"dolor","helpRequested":true,
                 "consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s"}
                """.formatted(mobileId, key);
        var response = mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        UUID consultationId = UUID.fromString(id);
        String storageKey = "lifeline/consultations/" + id;
        assertThat(eventsFor(consultationId)).hasSize(1);
        when(images.upload(any(byte[].class), any(UUID.class))).thenReturn(storageKey);

        mvc.perform(multipart("/api/v1/consultation-submissions/{id}/photo", id)
                        .file(photo()).header("Idempotency-Key", key.toString()))
                .andExpect(status().isOk());
        assertThat(eventsFor(consultationId)).hasSize(1);
    }

    private String createPendingPhotoConsultation(UUID key) throws Exception {
        String body = """
                {"mobileConsultationId":"%s","helpRequested":true,
                 "consultationRecordedAt":"2026-10-27T18:00:00","idempotencyKey":"%s"}
                """.formatted(UUID.randomUUID(), key);
        var response = mvc.perform(post("/api/v1/consultation-submissions")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.id");
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("file", "image.png", "image/png", PNG);
    }

    private java.util.List<com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.entities.OutboxEvent>
    eventsFor(UUID consultationId) {
        return events.findAll().stream().filter(event -> event.getAggregateId().equals(consultationId)).toList();
    }
}
