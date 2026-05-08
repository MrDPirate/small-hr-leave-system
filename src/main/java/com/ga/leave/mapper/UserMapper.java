package com.ga.leave.mapper;

import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.model.response.UserResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert entities to DTOs
 */
@Component
public class UserMapper {

    public UserResponseDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO.UserProfileDTO profileDTO = null;
        if (user.getUserProfile() != null) {
            profileDTO = toProfileDTO(user.getUserProfile());
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .emailAddress(user.getEmailAddress())
                .accountVerified(user.isAccountVerified())
                .active(user.isActive())
                .profile(profileDTO)
                .build();
    }

    public UserResponseDTO.UserProfileDTO toProfileDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        // Generate image URL for the profile
        String imageUrl = null;
        if (profile.getImageData() != null && profile.getImageData().length > 0) {
            imageUrl = "/api/v1/profile/" + profile.getId() + "/image";
        }

        return UserResponseDTO.UserProfileDTO.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .imageUrl(imageUrl)
                .build();
    }
}

