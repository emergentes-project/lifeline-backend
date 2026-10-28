package com.upc.pe.lifeline.casemanagement.interfaces.rest.controllers;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultation_photo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "authorization.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "cloudinary.enabled=false",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
@AutoConfigureMockMvc
class ConsultationPhotoControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ConsultationSubmissionRepository submissions;

    @MockitoBean
    private ImageStorageService images;

    @Test
    void aMissingConsultationReturnsNotFoundAndDoesNotUpload() throws Exception {
        mvc.perform(multipart("/api/v1/consultation-submissions/" + UUID.randomUUID() + "/photo")
                        .file(image()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(images, never()).upload(any(), any());
    }

    @Test
    void aConsultationWithoutAPhotoStoresTheUploadedKey() throws Exception {
        ConsultationSubmission submission = submissions.save(submission(null));
        String storageKey = "lifeline/consultations/" + submission.getId();
        when(images.upload(any(), eq(submission.getId()))).thenReturn(storageKey);

        mvc.perform(multipart("/api/v1/consultation-submissions/" + submission.getId() + "/photo")
                        .file(image()))
                .andExpect(status().isCreated())
                .andExpect(content().string(storageKey));

        verify(images).upload(any(), eq(submission.getId()));
        assertThat(submissions.findById(submission.getId()).orElseThrow().getPhotoStorageKey())
                .isEqualTo(storageKey);
    }

    @Test
    void aConsultationThatAlreadyHasAPhotoReturnsTheStoredKey() throws Exception {
        String storageKey = "lifeline/consultations/" + UUID.randomUUID();
        ConsultationSubmission submission = submissions.save(submission(storageKey));

        mvc.perform(multipart("/api/v1/consultation-submissions/" + submission.getId() + "/photo")
                        .file(image()))
                .andExpect(status().isOk())
                .andExpect(content().string(storageKey));

        verify(images, never()).upload(any(), any());
    }

    @Test
    void anEmptyFileIsRejectedBeforeUpload() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

        mvc.perform(multipart("/api/v1/consultation-submissions/" + UUID.randomUUID() + "/photo")
                        .file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(images, never()).upload(any(), any());
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
    }

    private static ConsultationSubmission submission(String photoStorageKey) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return new ConsultationSubmission(UUID.randomUUID(), null, "dolor", photoStorageKey, false,
                now, now, UUID.randomUUID(), null, null);
    }
}
