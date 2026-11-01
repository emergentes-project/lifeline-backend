package com.upc.pe.lifeline.notifications.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.upc.pe.lifeline.notifications.domain.model.CasePushMessage;
import com.upc.pe.lifeline.notifications.domain.model.NotificationSendResult;
import com.upc.pe.lifeline.notifications.domain.services.CaseNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class FirebaseCaseNotificationSender implements CaseNotificationSender {
    private final boolean enabled;
    private final String credentials;

    public FirebaseCaseNotificationSender(
            @Value("${lifeline.notifications.firebase.enabled:false}") boolean enabled,
            @Value("${FIREBASE_CREDENTIALS:}") String credentials) {
        this.enabled = enabled;
        this.credentials = credentials == null ? "" : credentials.trim();
    }

    @Override
    public NotificationSendResult send(CasePushMessage message, List<String> tokens) {
        if (!enabled || credentials.isEmpty() || tokens == null || tokens.isEmpty()) {
            return new NotificationSendResult(true, List.of());
        }
        List<Message> messages = new ArrayList<>();
        for (String token : tokens) {
            messages.add(Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
                    .putData("title", message.title())
                    .putData("body", message.body())
                    .putData("caseId", message.caseId().toString())
                    .putData("status", message.status())
                    .build());
        }
        try {
            return result(tokens, FirebaseMessaging.getInstance(app()).sendEach(messages));
        } catch (FirebaseMessagingException exception) {
            throw new IllegalStateException("Case notification could not be sent", exception);
        }
    }

    private NotificationSendResult result(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new ArrayList<>();
        boolean otherFailure = false;
        List<SendResponse> responses = response.getResponses();
        for (int index = 0; index < responses.size(); index++) {
            SendResponse item = responses.get(index);
            if (item.isSuccessful()) {
                continue;
            }
            if (invalidToken(item.getException())) {
                invalidTokens.add(tokens.get(index));
            } else {
                otherFailure = true;
            }
        }
        return new NotificationSendResult(!otherFailure, invalidTokens);
    }

    private boolean invalidToken(FirebaseMessagingException exception) {
        if (exception == null || exception.getMessagingErrorCode() == null) {
            return false;
        }
        MessagingErrorCode code = exception.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private FirebaseApp app() {
        synchronized (FirebaseCaseNotificationSender.class) {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }
            try {
                GoogleCredentials googleCredentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8)));
                return FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(googleCredentials)
                        .build());
            } catch (Exception exception) {
                throw new IllegalStateException("Firebase credentials could not be loaded", exception);
            }
        }
    }
}
