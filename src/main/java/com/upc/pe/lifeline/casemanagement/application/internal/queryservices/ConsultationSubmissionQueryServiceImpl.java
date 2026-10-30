package com.upc.pe.lifeline.casemanagement.application.internal.queryservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.FilterConsultationSubmissionsQuery;
import com.upc.pe.lifeline.casemanagement.domain.model.queries.GetConsultationSubmissionByIdQuery;
import com.upc.pe.lifeline.casemanagement.domain.services.ConsultationSubmissionQueryService;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ConsultationSubmissionQueryServiceImpl implements ConsultationSubmissionQueryService {
    private final ConsultationSubmissionRepository submissions;

    public ConsultationSubmissionQueryServiceImpl(ConsultationSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationSubmission handle(GetConsultationSubmissionByIdQuery query) {
        return submissions.findById(query.consultationId())
                .orElseThrow(() -> new NoSuchElementException("Consultation submission not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationSubmission> handle(FilterConsultationSubmissionsQuery query) {
        return submissions.findAllByOrderByReceivedAtDesc().stream()
                .filter(submission -> query.province() == null || submission.getPatient() != null
                        && query.province().equalsIgnoreCase(submission.getPatient().getProvince()))
                .filter(submission -> query.helpRequested() == null
                        || submission.isHelpRequested() == query.helpRequested())
                .toList();
    }
}
