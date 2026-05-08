package com.ga.leave.service;


import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.repository.UserProfileRepository;
import com.ga.leave.security.MyUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Semaphore to limit concurrent file uploads.
     * Allows max 3 concurrent image uploads to prevent server memory overload,
     * since each upload reads the entire file into memory for magic byte validation.
     */
    private static final Semaphore UPLOAD_SEMAPHORE = new Semaphore(3);

    // Allowed image MIME types
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Magic bytes for image validation
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] WEBP_MAGIC = {0x52, 0x49, 0x46, 0x46}; // RIFF header

    public UserProfile updateProfileImage(Long profileId, MultipartFile image) throws IOException {
        // Acquire semaphore permit to limit concurrent uploads
        try {
            UPLOAD_SEMAPHORE.acquire();
            log.debug("Upload permit acquired. Available permits: {}", UPLOAD_SEMAPHORE.availablePermits());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted while waiting for available slot");
        }

        try {
            UserProfile profile = userProfileRepository.findById(profileId)
                    .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Authorization: Verify the current user owns this profile
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getUserProfile() == null ||
                !currentUser.getUserProfile().getId().equals(profileId)) {
            throw new SecurityException("You are not authorized to update this profile");
        }

        // Validate file is not empty
        if (image.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file size
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        // Validate content type
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        // Validate magic bytes match claimed content type
        byte[] fileBytes = image.getBytes();
        if (!validateMagicBytes(fileBytes, contentType)) {
            throw new IllegalArgumentException("File content does not match declared type");
        }

        // Sanitize filename (remove path traversal attempts and special characters)
        String originalFilename = image.getOriginalFilename();
        String safeName = sanitizeFilename(originalFilename);

        profile.setImageName(safeName);
        profile.setImageType(contentType);
        profile.setImageData(fileBytes);

        return userProfileRepository.save(profile);
        } finally {
            UPLOAD_SEMAPHORE.release();
            log.debug("Upload permit released. Available permits: {}", UPLOAD_SEMAPHORE.availablePermits());
        }
    }

    public UserProfile getProfile(Long profileId) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getUserProfile() == null ||
                !currentUser.getUserProfile().getId().equals(profileId)) {
            throw new SecurityException("You are not authorized to view this profile");
        }

        return profile;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MyUserDetails) {
            return ((MyUserDetails) authentication.getPrincipal()).getUser();
        }
        return null;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "image";
        }
        // Remove path separators and null bytes
        String sanitized = filename
                .replaceAll("[/\\\\]", "")
                .replaceAll("\\.\\.", "")
                .replaceAll("[\\x00-\\x1F]", "")
                .replaceAll("[<>:\"|?*]", "_");

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized.isEmpty() ? "image" : sanitized;
    }

    private boolean validateMagicBytes(byte[] fileBytes, String contentType) {
        if (fileBytes == null || fileBytes.length < 4) {
            return false;
        }

        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> startsWith(fileBytes, JPEG_MAGIC);
            case "image/png" -> startsWith(fileBytes, PNG_MAGIC);
            case "image/gif" -> startsWith(fileBytes, GIF_MAGIC);
            case "image/webp" -> startsWith(fileBytes, WEBP_MAGIC);
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

