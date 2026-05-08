package com.ga.leave.controller;

import com.ga.leave.model.UserProfile;
import com.ga.leave.model.response.ApiResponse;
import com.ga.leave.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @PutMapping("/{profileId}/image")
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

    @GetMapping("/{profileId}/image")
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
