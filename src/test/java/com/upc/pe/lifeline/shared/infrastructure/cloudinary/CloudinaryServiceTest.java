package com.upc.pe.lifeline.shared.infrastructure.cloudinary;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CloudinaryServiceTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a1ZkAAAAASUVORK5CYII=");
    private static final UUID CONSULTATION_ID = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    private static final String STORAGE_KEY = "lifeline/consultations/" + CONSULTATION_ID;

    private Cloudinary cloudinary;
    private Uploader uploader;
    private Api api;
    private CloudinaryService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        api = mock(Api.class);
        service = new CloudinaryService(cloudinary, DataSize.ofMegabytes(10), Duration.ofMinutes(1));
    }

    @Test
    void uploadsAnAuthenticatedImageWithAStablePublicId() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq(PNG), anyMap())).thenReturn(Map.of(
                "public_id", STORAGE_KEY, "resource_type", "image", "type", "authenticated"));

        assertThat(service.upload(PNG, CONSULTATION_ID)).isEqualTo(STORAGE_KEY);
        assertThat(service.upload(PNG, CONSULTATION_ID)).isEqualTo(STORAGE_KEY);

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader, org.mockito.Mockito.times(2)).upload(eq(PNG), options.capture());
        assertThat(options.getValue()).containsEntry("overwrite", false)
                .containsEntry("type", "authenticated").containsEntry("public_id", STORAGE_KEY);
    }

    @Test
    void doesNotAcceptAnotherConsultationsKey() {
        assertThat(service.belongsToConsultation(STORAGE_KEY, UUID.randomUUID())).isFalse();
        verifyNoInteractions(cloudinary);
    }

    @Test
    void producesAShortLivedDownloadUrl() throws Exception {
        resource();
        when(cloudinary.privateDownload(eq(STORAGE_KEY), eq("png"), anyMap()))
                .thenReturn("https://api.cloudinary.com/signed-download");
        long before = Instant.now().getEpochSecond();

        assertThat(service.getDownloadUrl(STORAGE_KEY)).isEqualTo("https://api.cloudinary.com/signed-download");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(cloudinary).privateDownload(eq(STORAGE_KEY), eq("png"), options.capture());
        assertThat(options.getValue()).containsEntry("type", "authenticated");
        assertThat(((Number) options.getValue().get("expires_at")).longValue())
                .isBetween(before + 60, Instant.now().getEpochSecond() + 60);
    }

    private void resource() throws Exception {
        ApiResponse response = mock(ApiResponse.class);
        when(response.get("public_id")).thenReturn(STORAGE_KEY);
        when(response.get("resource_type")).thenReturn("image");
        when(response.get("type")).thenReturn("authenticated");
        when(response.get("format")).thenReturn("png");
        when(cloudinary.api()).thenReturn(api);
        when(api.resource(eq(STORAGE_KEY), anyMap())).thenReturn(response);
    }
}
