package com.upc.pe.lifeline.notifications.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.CaseRepository;
import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.model.NotificationSendResult;
import com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationDelivery;
import com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationToken;
import com.upc.pe.lifeline.notifications.domain.services.CaseNotificationSender;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationDeliveryRepository;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDeliveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryService.class);
    private final NotificationDeliveryRepository deliveries;
    private final NotificationTokenRepository tokens;
    private final CaseRepository cases;
    private final CaseNotificationSender sender;
    private final TransactionTemplate transactions;
    private final Duration initialDelay;
    private final int maxAttempts;

    public NotificationDeliveryService(NotificationDeliveryRepository deliveries, NotificationTokenRepository tokens,
                                       CaseRepository cases, CaseNotificationSender sender,
                                       PlatformTransactionManager transactions,
                                       @Value("${lifeline.notifications.delivery.initial-delay:PT5S}") Duration initialDelay,
                                       @Value("${lifeline.notifications.delivery.max-attempts:5}") int maxAttempts) {
        this.deliveries = deliveries;
        this.tokens = tokens;
        this.cases = cases;
        this.sender = sender;
        this.transactions = new TransactionTemplate(transactions);
        this.initialDelay = initialDelay;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void enqueue(UUID caseId, CaseStatus status) {
        if (deliveries.findByCaseIdAndStatus(caseId, status.name()).isPresent()) {
            return;
        }
        try {
            deliveries.saveAndFlush(new NotificationDelivery(caseId, status.name(), now()));
        } catch (DataIntegrityViolationException exception) {
            LOGGER.debug("Notification delivery already queued for case {}", caseId);
        }
    }

    public void dispatchDue() {
        List<UUID> due = deliveries.findDueIds(maxAttempts, now());
        for (UUID deliveryId : due) {
            try {
                transactions.executeWithoutResult(status -> dispatchOne(deliveryId));
            } catch (RuntimeException exception) {
                LOGGER.warn("Notification delivery could not be processed");
            }
        }
    }

    private void dispatchOne(UUID deliveryId) {
        NotificationDelivery delivery = deliveries.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getSentAt() != null || delivery.getAttempts() >= maxAttempts) {
            return;
        }
        CasePushMessage message = message(delivery.getStatus(), delivery.getCaseId());
        if (message == null) {
            delivery.markSent(now());
            return;
        }
        var caseEntity = cases.findById(delivery.getCaseId()).orElse(null);
        if (caseEntity == null) {
            delivery.markSent(now());
            return;
        }
        List<String> recipients = recipients(caseEntity.getConsultationSubmission());
        if (recipients.isEmpty()) {
            delivery.markSent(now());
            return;
        }
        try {
            NotificationSendResult result = sender.send(message, recipients);
            forgetInvalidTokens(result.invalidTokens());
            if (result.delivered()) {
                delivery.markSent(now());
            } else {
                delivery.recordFailure(nextAttempt(delivery.getAttempts()));
            }
        } catch (RuntimeException exception) {
            delivery.recordFailure(nextAttempt(delivery.getAttempts()));
            LOGGER.warn("Notification delivery attempt failed for case {}", delivery.getCaseId());
        }
    }

    private void forgetInvalidTokens(List<String> invalidTokens) {
        for (String invalidToken : invalidTokens) {
            tokens.findByToken(invalidToken).ifPresent(stored -> {
                tokens.delete(stored);
                LOGGER.warn("Removed an unregistered notification token");
            });
        }
    }

    private List<String> recipients(ConsultationSubmission consultation) {
        if (consultation.getCitizenUserId() != null) {
            return tokens.findByUserId(consultation.getCitizenUserId().userId()).stream()
                    .map(NotificationToken::getToken)
                    .toList();
        }
        return tokens.findByConsultationId(consultation.getId()).stream()
                .map(NotificationToken::getToken)
                .toList();
    }

    private CasePushMessage message(String status, UUID caseId) {
        return switch (status) {
            case "ASSIGNED" -> new CasePushMessage("Un profesional asumió tu caso",
                    "Tu solicitud ya tiene un profesional asignado.", caseId, status);
            case "IN_PROGRESS" -> new CasePushMessage("Tu caso está en atención",
                    "Un profesional inició la atención.", caseId, status);
            case "CLOSED" -> new CasePushMessage("Tu caso fue cerrado",
                    "La atención del caso terminó.", caseId, status);
            default -> null;
        };
    }

    private LocalDateTime nextAttempt(int attemptsAlreadyMade) {
        int shift = Math.min(Math.max(attemptsAlreadyMade, 0), 20);
        return now().plus(initialDelay.multipliedBy(1L << shift));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
