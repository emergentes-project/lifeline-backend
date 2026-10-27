package com.upc.pe.lifeline.shared.application.external;

import java.util.UUID;

public interface ImageStorageService {
    String upload(byte[] file, UUID consultationId);
    boolean belongsToConsultation(String storageKey, UUID consultationId);
    String getDownloadUrl(String storageKey);
    void delete(String storageKey);
}
