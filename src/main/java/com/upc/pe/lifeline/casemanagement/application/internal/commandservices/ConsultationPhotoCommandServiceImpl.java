package com.upc.pe.lifeline.casemanagement.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.UploadConsultationPhotoCommand;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationPhotoCommandService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.OutboxEventRepository;
import com.upc.pe.lifeline.shared.application.exceptions.ImageStorageException;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class ConsultationPhotoCommandServiceImpl implements ConsultationPhotoCommandService {
    private final ConsultationSubmissionRepository submissions;
    private final ObjectProvider<ImageStorageService> images;
    private final OutboxEventRepository events;

    public ConsultationPhotoCommandServiceImpl(ConsultationSubmissionRepository submissions,
                                               ObjectProvider<ImageStorageService> images,
                                               OutboxEventRepository events) {
        this.submissions = submissions;
        this.images = images;
        this.events = events;
    }

    @Override
    @Transactional
    public String handle(UploadConsultationPhotoCommand command) {
        ConsultationSubmission submission = submissions.findByIdForUpdate(command.consultationId())
                .orElseThrow(() -> new NoSuchElementException("Consultation submission not found"));
        String storedKey = submission.getPhotoStorageKey();
        if (storedKey != null && !storedKey.isBlank()) {
            return storedKey;
        }
        boolean wasPendingPhoto = submission.isPhotoPending();
        ImageStorageService storage = imageStorage();
        String storageKey = "lifeline/consultations/" + submission.getId();
        if (!storage.belongsToConsultation(storageKey, submission.getId())) {
            storageKey = storage.upload(command.image(), submission.getId());
        }
        submission.attachPhoto(storageKey);
        submissions.saveAndFlush(submission);
        if (wasPendingPhoto && submission.isHelpRequested()) {
            events.saveAndFlush(ConsultationReceivedOutboxEventFactory.create(submission));
        }
        return storageKey;
    }

    private ImageStorageService imageStorage() {
        ImageStorageService storage = images.getIfAvailable();
        if (storage == null) {
            throw new ImageStorageException("Image storage is not configured", null);
        }
        return storage;
    }
}
