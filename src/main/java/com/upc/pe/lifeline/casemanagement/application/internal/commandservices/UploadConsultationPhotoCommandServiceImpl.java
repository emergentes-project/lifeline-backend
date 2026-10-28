package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.UploadConsultationPhotoCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class UploadConsultationPhotoCommandServiceImpl implements UploadConsultationPhotoCommandService {
    private final ConsultationSubmissionRepository submissions;
    private final ObjectProvider<ImageStorageService> images;

    public UploadConsultationPhotoCommandServiceImpl(ConsultationSubmissionRepository submissions,
                                                     ObjectProvider<ImageStorageService> images) {
        this.submissions = submissions;
        this.images = images;
    }

    @Override
    @Transactional
    public PhotoUpload handle(UploadConsultationPhotoCommand command) {
        ConsultationSubmission submission = submissions.findById(command.consultationId())
                .orElseThrow(() -> new NoSuchElementException("Consultation not found"));
        String storedKey = submission.getPhotoStorageKey();
        if (storedKey != null && !storedKey.isBlank()) {
            return new PhotoUpload(storedKey, false);
        }
        String storageKey = images.getObject().upload(command.image(), command.consultationId());
        submission.setPhotoStorageKey(storageKey);
        return new PhotoUpload(storageKey, true);
    }
}
