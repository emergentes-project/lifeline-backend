package com.upc.pe.lifeline.notifications.domain.model;

import java.util.List;

public record NotificationSendResult(boolean delivered, List<String> invalidTokens) {
    public NotificationSendResult {
        invalidTokens = invalidTokens == null ? List.of() : List.copyOf(invalidTokens);
    }
}
