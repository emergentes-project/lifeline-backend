package com.upc.pe.lifeline.notifications.application.internal.commandservices;

import com.upc.pe.lifeline.casemanagement.domain.model.aggregates.ConsultationSubmission;
import com.upc.pe.lifeline.casemanagement.infrastructure.persistence.jpa.repositories.ConsultationSubmissionRepository;
import com.upc.pe.lifeline.notifications.domain.exceptions.NotificationRegistrationException;
import com.upc.pe.lifeline.notifications.domain.model.aggregates.NotificationToken;
import com.upc.pe.lifeline.notifications.domain.services.NotificationTokenCommandService;
import com.upc.pe.lifeline.notifications.infrastructure.persistence.jpa.repositories.NotificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationTokenCommandServiceImpl implements NotificationTokenCommandService {
    private final NotificationTokenRepository tokens;
    private final ConsultationSubmissionRepository consultations;

    public NotificationTokenCommandServiceImpl(NotificationTokenRepository tokens,
                                                ConsultationSubmissionRepository consultations) {
        this.tokens = tokens;
        this.consultations = consultations;
    }

    @Override
    @Transactional
    public void register(String rawToken, UUID consultationId, UUID citizenUserId) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.isEmpty() || token.length() > 255) {
            throw new IllegalArgumentException("Notification token is required");
        }
        if (citizenUserId != null) {
            if (consultationId != null) {
                requireOwnedBy(consultationId, citizenUserId);
            }
            store(token, citizenUserId, consultationId);
            return;
        }
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation is required");
        }
        ConsultationSubmission consultation = requireConsultation(consultationId);
        if (!consultation.isHelpRequested() || consultation.getCitizenUserId() != null) {
            throw new NotificationRegistrationException("The consultation cannot register a guest token");
        }
        store(token, null, consultationId);
    }

    private void requireOwnedBy(UUID consultationId, UUID citizenUserId) {
        ConsultationSubmission consultation = requireConsultation(consultationId);
        UUID owner = consultation.getCitizenUserId() == null ? null : consultation.getCitizenUserId().userId();
        if (!citizenUserId.equals(owner)) {
            throw new NotificationRegistrationException("The consultation does not belong to this citizen");
        }
    }

    private ConsultationSubmission requireConsultation(UUID consultationId) {
        return consultations.findById(consultationId)
                .orElseThrow(() -> new NoSuchElementException("Consultation not found"));
    }

    private void store(String token, UUID userId, UUID consultationId) {
        LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        NotificationToken stored = tokens.findByToken(token)
                .orElseGet(() -> new NotificationToken(token, userId, consultationId, updatedAt));
        stored.replaceOwner(userId, consultationId, updatedAt);
        tokens.save(stored);
    }
}
