package com.upc.pe.lifeline.casemanagement.application.internal.queryservices;

import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationPhotoUrlQuery;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationPhotoQueryService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.shared.application.exceptions.ImageStorageException;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class ConsultationPhotoQueryServiceImpl implements ConsultationPhotoQueryService {
    private final ConsultationSubmissionRepository submissions;
    private final ObjectProvider<ImageStorageService> images;

    public ConsultationPhotoQueryServiceImpl(ConsultationSubmissionRepository submissions,
                                             ObjectProvider<ImageStorageService> images) {
        this.submissions = submissions;
        this.images = images;
    }

    @Override
    @Transactional(readOnly = true)
    public String handle(GetConsultationPhotoUrlQuery query) {
        var submission = submissions.findById(query.consultationId())
                .orElseThrow(() -> new NoSuchElementException("Consultation submission not found"));
        if (submission.getPhotoStorageKey() == null) {
            throw new NoSuchElementException("Consultation photo not found");
        }
        ImageStorageService storage = images.getIfAvailable();
        if (storage == null) {
            throw new ImageStorageException("Image storage is not configured", null);
        }
        if (!storage.belongsToConsultation(submission.getPhotoStorageKey(), submission.getId())) {
            throw new NoSuchElementException("Consultation photo not found");
        }
        return storage.getDownloadUrl(submission.getPhotoStorageKey());
    }
}
