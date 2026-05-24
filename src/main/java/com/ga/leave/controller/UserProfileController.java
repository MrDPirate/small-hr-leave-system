package com.ga.leave.controller;

import com.ga.leave.mapper.UserMapper;
import com.ga.leave.model.UserProfile;
import com.ga.leave.model.request.AssignManagerRequest;
import com.ga.leave.model.request.UpdateProfileRequest;
import com.ga.leave.model.response.ApiResponse;
import com.ga.leave.model.response.UserResponseDTO;
import com.ga.leave.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for user profile management.
 * Handles profile retrieval, updates, profile image upload/download, and manager assignment.
 *
 * <p>Base path: {@code /api/v1/profile}</p>
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Profile management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserMapper userMapper;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return 200 with the current user's profile DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's own profile")
    public ResponseEntity<ApiResponse<UserResponseDTO.UserProfileDTO>> getMyProfile() {
        UserProfile profile = userProfileService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success(userMapper.toProfileDTO(profile)));
    }

    /**
     * Updates profile fields (firstName, lastName, phoneNumber) for the given profile.
     * Users may only update their own profile.
     *
     * @param profileId the ID of the profile to update
     * @param request   the fields to update (all optional)
     * @return 200 with the updated profile DTO
     */
    @PutMapping("/{profileId}")
    @Operation(summary = "Update profile", description = "Updates the authenticated user's own profile fields")
    public ResponseEntity<ApiResponse<UserResponseDTO.UserProfileDTO>> updateProfile(
            @PathVariable Long profileId,
            @RequestBody UpdateProfileRequest request) {
        UserProfile updated = userProfileService.updateProfile(profileId, request);
        return ResponseEntity.ok(ApiResponse.success(userMapper.toProfileDTO(updated), "Profile updated successfully"));
    }

    /**
     * Assigns a manager to the specified employee profile.
     * Restricted to HR admins (ROLE_ADMIN).
     *
     * @param profileId the employee profile ID to receive the manager assignment
     * @param request   contains the manager's profile ID
     * @return 200 with the updated employee profile DTO
     */
    @PutMapping("/{profileId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign manager", description = "HR admin only — assigns a manager to an employee profile")
    public ResponseEntity<ApiResponse<UserResponseDTO.UserProfileDTO>> assignManager(
            @PathVariable Long profileId,
            @Valid @RequestBody AssignManagerRequest request) {
        UserProfile updated = userProfileService.assignManager(profileId, request);
        return ResponseEntity.ok(ApiResponse.success(userMapper.toProfileDTO(updated), "Manager assigned successfully"));
    }

    /**
     * Uploads a profile image for the given profile.
     * Users may only upload an image for their own profile.
     * Validates file size (max 5MB), MIME type, and magic bytes.
     *
     * @param profileId the ID of the profile to update
     * @param image     the image file to upload (JPEG, PNG, GIF, or WebP)
     * @return 200 on success
     */
    @PutMapping("/{profileId}/image")
    @Operation(summary = "Upload profile image", description = "Users can upload their own profile picture")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @PathVariable Long profileId,
            @RequestParam("image") MultipartFile image
    ) {
        try {
            userProfileService.updateProfileImage(profileId, image);
            return ResponseEntity.ok(ApiResponse.success(null, "Profile image updated successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred while uploading the image"));
        }
    }

    /**
     * Downloads the profile image for the given profile.
     * Users may only download their own profile image.
     *
     * @param profileId the ID of the profile whose image is requested
     * @return 200 with raw image bytes and appropriate Content-Type header
     */
    @GetMapping("/{profileId}/image")
    @Operation(summary = "Download profile image", description = "Returns the raw profile image bytes")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long profileId) {
        try {
            UserProfile profile = userProfileService.getProfile(profileId);

            if (profile.getImageData() == null || profile.getImageData().length == 0) {
                return ResponseEntity.notFound().build();
            }

            String contentType = profile.getImageType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header("Content-Disposition", "inline")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(profile.getImageData());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
