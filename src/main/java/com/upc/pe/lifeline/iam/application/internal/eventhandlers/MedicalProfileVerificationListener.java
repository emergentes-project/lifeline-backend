package com.upc.pe.lifeline.iam.application.internal.eventhandlers;

import com.upc.pe.lifeline.iam.application.internal.commandservices.MedicalProfileVerificationService;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MedicalProfileVerificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalProfileVerificationListener.class);
    private final MedicalProfileVerificationService verificationService;

    public MedicalProfileVerificationListener(MedicalProfileVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MedicalProfessionalRegisteredEvent event) {
        try {
            verificationService.verify(event.userId());
        } catch (RuntimeException exception) {
            LOGGER.error("Medical profile verification failed for user {}", event.userId(), exception);
        }
    }
}
