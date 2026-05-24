package com.ga.leave.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for assigning a manager to a user profile.
 * Only HR admins (ROLE_ADMIN) may use this endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignManagerRequest {

    /**
     * Profile ID of the user to be set as the manager.
     * The target profile must exist. Pass null to remove the manager assignment.
     */
    @NotNull(message = "Manager profile ID is required")
    private Long managerId;
}
