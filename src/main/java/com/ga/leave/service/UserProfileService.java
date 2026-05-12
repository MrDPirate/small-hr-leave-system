package com.ga.leave.service;


import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.model.request.AssignManagerRequest;
import com.ga.leave.model.request.UpdateProfileRequest;
import com.ga.leave.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * Service for managing user profile data including profile image uploads,
 * profile field updates, and manager assignment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserContextService userContextService;

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
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC  = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] WEBP_MAGIC = {0x52, 0x49, 0x46, 0x46}; // RIFF header

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return the current user's UserProfile
     * @throws IllegalStateException if no authenticated user is found
     */
    public UserProfile getMyProfile() {
        return userContextService.getCurrentProfile();
    }

    /**
     * Updates editable profile fields (firstName, lastName, phoneNumber).
     * Only non-null fields in the request are applied.
     * Users may only update their own profile.
     *
     * @param profileId the ID of the profile to update
     * @param request   the fields to update
     * @return the updated UserProfile
     * @throws SecurityException            if the caller does not own this profile
     * @throws InformationNotFoundException if the profile does not exist
     */
    @Transactional
    public UserProfile updateProfile(Long profileId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new InformationNotFoundException("Profile with id " + profileId + " not found"));

        User currentUser = userContextService.getCurrentUser();
        if (currentUser == null || currentUser.getUserProfile() == null
                || !currentUser.getUserProfile().getId().equals(profileId)) {
            throw new SecurityException("You are not authorized to update this profile");
        }

        if (request.getFirstName() != null)   profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    profile.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());

        return userProfileRepository.save(profile);
    }

    /**
     * Assigns or re-assigns a manager to the given employee profile.
     * Only HR admins may call this; enforcement is done at the controller level.
     *
     * @param profileId the employee profile to update
     * @param request   contains the manager's profile ID
     * @return the updated employee UserProfile
     * @throws InformationNotFoundException if either profile does not exist
     */
    @Transactional
    public UserProfile assignManager(Long profileId, AssignManagerRequest request) {
        UserProfile employeeProfile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new InformationNotFoundException("Profile with id " + profileId + " not found"));

        UserProfile managerProfile = userProfileRepository.findById(request.getManagerId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Manager profile with id " + request.getManagerId() + " not found"));

        employeeProfile.setManager(managerProfile);
        UserProfile updated = userProfileRepository.save(employeeProfile);
        log.info("Assigned manager id={} to employee profile id={}", managerProfile.getId(), profileId);
        return updated;
    }

    /**
     * Uploads and persists a profile image for the given profile.
     * Applies concurrent upload limiting, size checks, MIME type validation,
     * magic byte validation, and filename sanitization.
     *
     * @param profileId the ID of the profile to update
     * @param image     the multipart image file
     * @return the updated UserProfile
     * @throws IOException              if reading the file bytes fails
     * @throws SecurityException        if the caller does not own this profile
     * @throws IllegalArgumentException if the file is empty, too large, or of an unsupported type
     */
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
            User currentUser = userContextService.getCurrentUser();
            if (currentUser == null || currentUser.getUserProfile() == null ||
                    !currentUser.getUserProfile().getId().equals(profileId)) {
                throw new SecurityException("You are not authorized to update this profile");
            }

            if (image.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            if (image.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
            }

            String contentType = image.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
            }

            byte[] fileBytes = image.getBytes();
            if (!validateMagicBytes(fileBytes, contentType)) {
                throw new IllegalArgumentException("File content does not match declared type");
            }

            String safeName = sanitizeFilename(image.getOriginalFilename());
            profile.setImageName(safeName);
            profile.setImageType(contentType);
            profile.setImageData(fileBytes);

            return userProfileRepository.save(profile);
        } finally {
            UPLOAD_SEMAPHORE.release();
            log.debug("Upload permit released. Available permits: {}", UPLOAD_SEMAPHORE.availablePermits());
        }
    }

    /**
     * Retrieves a profile by ID after verifying the caller owns it.
     *
     * @param profileId the ID of the profile to retrieve
     * @return the requested UserProfile
     * @throws SecurityException if the caller does not own this profile
     * @throws RuntimeException  if the profile is not found
     */
    public UserProfile getProfile(Long profileId) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        User currentUser = userContextService.getCurrentUser();
        if (currentUser == null || currentUser.getUserProfile() == null ||
                !currentUser.getUserProfile().getId().equals(profileId)) {
            throw new SecurityException("You are not authorized to view this profile");
        }

        return profile;
    }

    /**
     * Removes dangerous characters from a filename to prevent path traversal attacks.
     *
     * @param filename the original filename from the uploaded file
     * @return a sanitized filename safe for storage, defaulting to "image" if blank
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "image";
        }
        String sanitized = filename
                .replaceAll("[/\\\\]", "")
                .replaceAll("\\.\\.", "")
                .replaceAll("[\\x00-\\x1F]", "")
                .replaceAll("[<>:\"|?*]", "_");

        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized.isEmpty() ? "image" : sanitized;
    }

    /**
     * Validates that the first bytes of the file match the expected magic bytes
     * for the declared MIME type, preventing content-type spoofing.
     *
     * @param fileBytes   the raw bytes of the uploaded file
     * @param contentType the declared MIME type
     * @return true if the magic bytes match the claimed type
     */
    private boolean validateMagicBytes(byte[] fileBytes, String contentType) {
        if (fileBytes == null || fileBytes.length < 4) {
            return false;
        }

        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> startsWith(fileBytes, JPEG_MAGIC);
            case "image/png"  -> startsWith(fileBytes, PNG_MAGIC);
            case "image/gif"  -> startsWith(fileBytes, GIF_MAGIC);
            case "image/webp" -> startsWith(fileBytes, WEBP_MAGIC);
            default -> false;
        };
    }

    /**
     * Checks whether the given byte array starts with the specified prefix.
     *
     * @param data   the full byte array
     * @param prefix the expected leading bytes
     * @return true if data begins with prefix
     */
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
