package com.upc.pe.lifeline.casemanagement.application.internal.eventhandlers;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.services.CaseNotificationSender;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashSet;
import java.util.List;

@Component
public class CaseStatusNotificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStatusNotificationListener.class);
    private final CaseRepository cases;
    private final NotificationTokenRepository tokens;
    private final CaseNotificationSender sender;
    private final TransactionTemplate transactions;

    public CaseStatusNotificationListener(CaseRepository cases, NotificationTokenRepository tokens,
                                          CaseNotificationSender sender, PlatformTransactionManager transactions) {
        this.cases = cases;
        this.tokens = tokens;
        this.sender = sender;
        this.transactions = new TransactionTemplate(transactions);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CaseStatusChangedEvent event) {
        try {
            if (!shouldNotify(event)) {
                return;
            }
            transactions.executeWithoutResult(status -> deliver(event));
        } catch (RuntimeException exception) {
            LOGGER.error("Case notification failed for case {}", event.caseId(), exception);
        }
    }

    private boolean shouldNotify(CaseStatusChangedEvent event) {
        if (event.previousStatus() == event.newStatus()) {
            return false;
        }
        return event.newStatus() == CaseStatus.ASSIGNED || event.newStatus() == CaseStatus.IN_PROGRESS;
    }

    private void deliver(CaseStatusChangedEvent event) {
        var caseEntity = cases.findById(event.caseId()).orElse(null);
        if (caseEntity == null) {
            return;
        }
        List<String> recipients = recipients(caseEntity.getConsultationSubmission());
        if (recipients.isEmpty()) {
            return;
        }
        sender.send(new CasePushMessage(title(event.newStatus()), body(event.newStatus()),
                event.caseId(), event.newStatus().name()), recipients);
    }

    private List<String> recipients(ConsultationSubmission consultation) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        if (consultation.getCitizenUserId() != null) {
            tokens.findByUserId(consultation.getCitizenUserId().userId())
                    .forEach(token -> found.add(token.getToken()));
        }
        tokens.findByConsultationId(consultation.getId()).forEach(token -> found.add(token.getToken()));
        return List.copyOf(found);
    }

    private String title(CaseStatus status) {
        return status == CaseStatus.ASSIGNED
                ? "Un profesional asumió tu caso"
                : "Tu caso está en atención";
    }

    private String body(CaseStatus status) {
        return status == CaseStatus.ASSIGNED
                ? "Tu solicitud ya tiene un profesional asignado."
                : "Un profesional inició la atención.";
    }
}
