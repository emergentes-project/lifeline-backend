package com.upc.pe.lifeline.casemanagement.domain.model.commands;

import java.util.UUID;

public record UploadConsultationPhotoCommand(UUID consultationId, byte[] image, String contentType) {
    public UploadConsultationPhotoCommand {
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation ID is required");
        }
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Image is required");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type is required");
        }
        image = image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }
}
