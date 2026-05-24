package com.ga.leave.features.leavetype.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating or updating a leave type.
 * Only HR admins (ROLE_ADMIN) may submit this.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeRequest {

    /** Unique display name for the leave category (e.g., "Annual Leave"). */
    @NotBlank(message = "Leave type name is required")
    private String name;

    /**
     * Default number of days employees receive for this leave type per year.
     * Must be a positive integer.
     */
    @Positive(message = "Default days must be a positive number")
    private int defaultDays;
}
