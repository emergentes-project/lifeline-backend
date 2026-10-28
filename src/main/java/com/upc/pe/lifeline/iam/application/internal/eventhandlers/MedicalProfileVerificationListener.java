package com.upc.pe.lifeline.iam.application.internal.eventhandlers;

import com.upc.pe.lifeline.iam.application.internal.commandservices.MedicalProfileVerificationService;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalRegisteredEvent;
import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalLicenseUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class MedicalProfileVerificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalProfileVerificationListener.class);
    private final MedicalProfileVerificationService verificationService;

    public MedicalProfileVerificationListener(MedicalProfileVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MedicalProfessionalRegisteredEvent event) {
        verify(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MedicalProfessionalLicenseUpdatedEvent event) {
        verify(event.userId());
    }

    private void verify(UUID userId) {
        try {
            verificationService.verify(userId);
        } catch (RuntimeException exception) {
            LOGGER.error("Medical profile verification failed for user {}", userId, exception);
        }
    }
}
