package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;

public interface ConsultationPhotoCommandService {
    String handle(UploadConsultationPhotoCommand command);
}
