package com.upc.pe.lifeline.shared.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.NotFound;
import com.upc.pe.lifeline.shared.application.exceptions.ImageStorageException;
import com.upc.pe.lifeline.shared.application.external.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CloudinaryService implements ImageStorageService {
    private static final String PREFIX = "lifeline/consultations/";
    private static final Pattern STORAGE_KEY = Pattern.compile(
            "lifeline/consultations/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final List<String> FORMATS = List.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;
    private final long maxFileSize;
    private final Duration downloadUrlDuration;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${cloudinary.max-file-size:10MB}") DataSize maxFileSize,
            @Value("${cloudinary.download-url-duration:PT1M}") Duration downloadUrlDuration) {
        if (maxFileSize.toBytes() <= 0 || downloadUrlDuration.toSeconds() <= 0) {
            throw new IllegalArgumentException("Image size and download URL duration must be positive");
        }
        this.cloudinary = cloudinary;
        this.maxFileSize = maxFileSize.toBytes();
        this.downloadUrlDuration = downloadUrlDuration;
    }

    @Override
    public String upload(byte[] file, UUID consultationId) {
        if (consultationId == null) {
            throw new IllegalArgumentException("Consultation ID is required");
        }
        validateImage(file);
        String publicId = PREFIX + consultationId;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file, Map.of(
                    "public_id", publicId,
                    "resource_type", "image",
                    "type", "authenticated",
                    "overwrite", false,
                    "unique_filename", false,
                    "use_filename", false,
                    "allowed_formats", FORMATS));
            if (!publicId.equals(result.get("public_id"))
                    || !"authenticated".equals(result.get("type"))
                    || !"image".equals(result.get("resource_type"))) {
                throw new IllegalStateException("Unexpected image storage response");
            }
            return publicId;
        } catch (Exception exception) {
            throw new ImageStorageException("Image upload failed", exception);
        }
    }

    @Override
    public boolean belongsToConsultation(String storageKey, UUID consultationId) {
        if (consultationId == null || !(PREFIX + consultationId).equals(storageKey)) {
            return false;
        }
        try {
            resource(storageKey);
            return true;
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    @Override
    public String getDownloadUrl(String storageKey) {
        Map<?, ?> resource = resource(storageKey);
        Object format = resource.get("format");
        if (!(format instanceof String imageFormat) || !FORMATS.contains(imageFormat)) {
            throw new ImageStorageException("Image format could not be resolved", null);
        }
        try {
            return cloudinary.privateDownload(storageKey, imageFormat, Map.of(
                    "resource_type", "image",
                    "type", "authenticated",
                    "attachment", false,
                    "expires_at", LocalDateTime.now(ZoneOffset.UTC).plus(downloadUrlDuration).toEpochSecond(ZoneOffset.UTC)));
        } catch (Exception exception) {
            throw new ImageStorageException("Image download URL could not be generated", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        validateStorageKey(storageKey);
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(storageKey, Map.of(
                    "resource_type", "image",
                    "type", "authenticated",
                    "invalidate", true));
            if (!List.of("ok", "not found").contains(result.get("result"))) {
                throw new IllegalStateException("Unexpected image deletion response");
            }
        } catch (Exception exception) {
            throw new ImageStorageException("Image deletion failed", exception);
        }
    }

    private Map<?, ?> resource(String storageKey) {
        validateStorageKey(storageKey);
        try {
            Map<?, ?> resource = cloudinary.api().resource(storageKey, Map.of(
                    "resource_type", "image", "type", "authenticated"));
            if (!storageKey.equals(resource.get("public_id"))
                    || !"authenticated".equals(resource.get("type"))
                    || !"image".equals(resource.get("resource_type"))) {
                throw new NoSuchElementException("Image not found");
            }
            return resource;
        } catch (NotFound exception) {
            throw new NoSuchElementException("Image not found");
        } catch (NoSuchElementException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ImageStorageException("Image could not be retrieved", exception);
        }
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Invalid image storage key");
        }
    }

    private void validateImage(byte[] file) {
        if (file == null || file.length == 0) {
            throw new IllegalArgumentException("Image is required");
        }
        if (file.length > maxFileSize) {
            throw new IllegalArgumentException("Image exceeds the maximum allowed size");
        }
        boolean jpeg = file.length >= 3 && file[0] == (byte) 0xff
                && file[1] == (byte) 0xd8 && file[2] == (byte) 0xff;
        boolean png = file.length >= 8 && file[0] == (byte) 0x89
                && file[1] == 'P' && file[2] == 'N' && file[3] == 'G'
                && file[4] == 13 && file[5] == 10 && file[6] == 26 && file[7] == 10;
        boolean webp = file.length >= 12
                && new String(file, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(file, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
        if (!jpeg && !png && !webp) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are supported");
        }
    }
}
