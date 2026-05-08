package com.ga.leave.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User response DTO - excludes sensitive data like password
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String userName;
    private String emailAddress;
    private boolean accountVerified;
    private boolean active;
    private UserProfileDTO profile;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfileDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String imageUrl;
    }
}

