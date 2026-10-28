package com.upc.pe.lifeline.casemanagement.infrastructure.messaging;

public interface CaseJobMessageSender {
    void send(String body);
}
