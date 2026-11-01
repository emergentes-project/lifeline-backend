package com.upc.pe.lifeline.casemanagement.application.internal.eventhandlers;

import com.upc.pe.lifeline.casemanagement.domain.model.events.CaseStatusChangedEvent;
import com.upc.pe.lifeline.casemanagement.domain.model.valueobjects.CaseStatus;
import com.upc.pe.lifeline.notifications.application.internal.commandservices.NotificationDeliveryService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CaseStatusNotificationListener {
    private final NotificationDeliveryService deliveries;

    public CaseStatusNotificationListener(NotificationDeliveryService deliveries) {
        this.deliveries = deliveries;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(CaseStatusChangedEvent event) {
        if (event.previousStatus() == event.newStatus()) {
            return;
        }
        if (event.newStatus() != CaseStatus.ASSIGNED
                && event.newStatus() != CaseStatus.IN_PROGRESS
                && event.newStatus() != CaseStatus.CLOSED) {
            return;
        }
        deliveries.enqueue(event.caseId(), event.newStatus());
    }
}
