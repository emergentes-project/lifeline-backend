package com.upc.pe.lifeline.shared.application.ports;

import java.time.Instant;

public interface AccessTokenRevocationStore {
    boolean isRevoked(String token);
    void revoke(String token, Instant expiresAt);
}
