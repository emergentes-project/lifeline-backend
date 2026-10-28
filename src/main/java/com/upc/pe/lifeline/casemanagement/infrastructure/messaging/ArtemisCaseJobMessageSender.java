package com.upc.pe.lifeline.casemanagement.infrastructure.messaging;

import jakarta.jms.DeliveryMode;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ArtemisCaseJobMessageSender implements CaseJobMessageSender {
    private final JmsTemplate jmsTemplate;
    private final String destination;

    public ArtemisCaseJobMessageSender(JmsTemplate jmsTemplate, OutboxPublisherProperties properties) {
        this.jmsTemplate = jmsTemplate;
        this.destination = properties.destination();
        this.jmsTemplate.setExplicitQosEnabled(true);
        this.jmsTemplate.setDeliveryPersistent(true);
    }

    @Override
    public void send(String body) {
        jmsTemplate.send(destination, session -> {
            var message = session.createTextMessage(body);
            message.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            return message;
        });
    }
}
