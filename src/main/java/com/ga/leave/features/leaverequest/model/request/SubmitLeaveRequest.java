package com.ga.leave.features.leaverequest.model.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request body for submitting a new leave request.
 * Submitted by an authenticated employee (ROLE_USER).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitLeaveRequest {

    /** ID of the leave type being requested (e.g., Annual, Sick). */
    @NotNull(message = "Leave type ID is required")
    private Long leaveTypeId;

    /** First day of the leave period. Must be a future or present date. */
    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    /** Last day of the leave period. Must be >= startDate. */
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /** Employee's reason or notes for the leave request. */
    @NotBlank(message = "Reason is required")
    private String reason;
}
