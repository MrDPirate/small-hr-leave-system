package com.ga.leave.service;

import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.security.MyUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Shared utility service for resolving the currently authenticated user
 * from the Spring Security context.
 *
 * <p>Centralises the SecurityContext access pattern so individual services
 * do not each replicate the same boilerplate.</p>
 */
@Service
public class UserContextService {

    /**
     * Returns the {@link User} entity associated with the current JWT session.
     *
     * @return the authenticated user, or null if no authentication is present
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MyUserDetails userDetails) {
            return userDetails.getUser();
        }
        return null;
    }

    /**
     * Returns the {@link UserProfile} of the currently authenticated user.
     *
     * @return the current user's profile
     * @throws IllegalStateException if the user or their profile cannot be resolved
     */
    public UserProfile getCurrentProfile() {
        User user = getCurrentUser();
        if (user == null || user.getUserProfile() == null) {
            throw new IllegalStateException("No authenticated user or user profile found");
        }
        return user.getUserProfile();
    }
}
