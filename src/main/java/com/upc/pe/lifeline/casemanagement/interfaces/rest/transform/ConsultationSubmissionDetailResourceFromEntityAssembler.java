package com.upc.pe.lifeline.casemanagement.interfaces.rest.transform;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.ConsultationAnswer;
import com.upc.pe.lifeline.casemanagement.domain.model.entities.Patient;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ConsultationSubmissionDetailResource;

public class ConsultationSubmissionDetailResourceFromEntityAssembler {
    public static ConsultationSubmissionDetailResource toResourceFromEntity(ConsultationSubmission submission) {
        Patient patient = submission.getPatient();
        ConsultationAnswer answer = submission.getAnswer();
        return new ConsultationSubmissionDetailResource(submission.getId(), submission.getMobileConsultationId(),
                submission.getCitizenUserId() == null ? null : submission.getCitizenUserId().userId(),
                submission.getInputText(), submission.getPhotoStorageKey() != null,
                submission.isHelpRequested(), submission.getConsultationRecordedAt(), submission.getReceivedAt(),
                patient == null ? null : new ConsultationSubmissionDetailResource.PatientResource(
                        patient.getName(), patient.getAgeYears(), patient.getLocationDescription(),
                        patient.getProvince(), patient.getLatitude(), patient.getLongitude()),
                answer == null ? null : new ConsultationSubmissionDetailResource.AnswerResource(
                        answer.getGuidanceText(), answer.getGuidanceStatus().name(),
                        answer.getPreliminaryPriority() == null ? null : answer.getPreliminaryPriority().name(),
                        answer.isProfessionalHelpRecommended(), answer.getSourceReferences()));
    }
}
