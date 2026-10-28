package com.upc.pe.lifeline.casemanagement.interfaces.rest.transform;

import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;
import com.upc.pe.lifeline.casemanagement.interfaces.rest.resources.ReceiveConsultationSubmissionResource;

public class ReceiveConsultationSubmissionCommandFromResourceAssembler {
    public static ReceiveConsultationSubmissionCommand toCommandFromResource(
            ReceiveConsultationSubmissionResource resource) {
        return new ReceiveConsultationSubmissionCommand(
                resource.mobileConsultationId(),
                resource.citizenUserId(),
                resource.inputText(),
                resource.photoStorageKey(),
                resource.helpRequested(),
                patient(resource.patient()),
                answer(resource.answer()),
                resource.consultationRecordedAt(),
                resource.idempotencyKey());
    }

    private static ReceiveConsultationSubmissionCommand.PatientData patient(
            ReceiveConsultationSubmissionResource.PatientResource patient) {
        if (patient == null) {
            return null;
        }
        return new ReceiveConsultationSubmissionCommand.PatientData(
                patient.name(), patient.ageYears(), patient.locationDescription(),
                patient.province(), patient.latitude(), patient.longitude());
    }

    private static ReceiveConsultationSubmissionCommand.AnswerData answer(
            ReceiveConsultationSubmissionResource.AnswerResource answer) {
        if (answer == null) {
            return null;
        }
        return new ReceiveConsultationSubmissionCommand.AnswerData(
                answer.guidanceText(), answer.guidanceStatus(), answer.preliminaryPriority(),
                answer.professionalHelpRecommended(), answer.sourceReferences());
    }
}
