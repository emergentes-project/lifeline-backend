package com.upc.pe.lifeline.notifications.domain.services;

import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.model.NotificationSendResult;

import java.util.List;

public interface CaseNotificationSender {
    NotificationSendResult send(CasePushMessage message, List<String> tokens);
}
