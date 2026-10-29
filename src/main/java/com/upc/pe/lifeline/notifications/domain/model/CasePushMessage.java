package com.upc.pe.lifeline.notifications.domain.model;

import java.util.UUID;

public record CasePushMessage(String title, String body, UUID caseId, String status) {
}
