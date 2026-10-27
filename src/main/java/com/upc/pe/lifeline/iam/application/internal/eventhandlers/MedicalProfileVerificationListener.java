package com.upc.pe.lifeline.iam.application.internal.eventhandlers;

import com.upc.pe.lifeline.iam.domain.model.events.MedicalProfessionalRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MedicalProfileVerificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalProfileVerificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MedicalProfessionalRegisteredEvent event) {
        LOGGER.info("Medical profile verification pending for user {}", event.userId());
    }
}
