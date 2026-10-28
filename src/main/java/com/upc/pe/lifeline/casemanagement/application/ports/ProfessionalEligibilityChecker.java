package com.upc.pe.lifeline.casemanagement.application.ports;

import java.util.UUID;

public interface ProfessionalEligibilityChecker {
    boolean isApproved(UUID professionalUserId);
}
