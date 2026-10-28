package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;

public interface UploadConsultationPhotoCommandService {
    PhotoUpload handle(UploadConsultationPhotoCommand command);

    record PhotoUpload(String storageKey, boolean created) {
    }
}
