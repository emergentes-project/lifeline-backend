package com.upc.pe.lifeline.casemanagement.domain.services;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.commands.ReceiveConsultationSubmissionCommand;

public interface ReceiveConsultationSubmissionCommandService {
    Reception handle(ReceiveConsultationSubmissionCommand command);

    record Reception(ConsultationSubmission submission, boolean created) {
        public static Reception created(ConsultationSubmission submission) {
            return new Reception(submission, true);
        }

        public static Reception replay(ConsultationSubmission submission) {
            return new Reception(submission, false);
        }
    }
}
