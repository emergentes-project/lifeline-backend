package com.upc.pe.lifeline.notifications.domain.services;

import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;

import java.util.List;

public interface CaseNotificationSender {
    void send(CasePushMessage message, List<String> tokens);
}
