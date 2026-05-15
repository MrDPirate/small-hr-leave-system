package com.ga.leave.features.leaverequest.model.response;

import com.ga.leave.features.leaverequest.model.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a leave request.
 * Contains all fields needed by clients without exposing internal entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {

    /** Unique identifier of the leave request. */
    private Long id;

    /** ID of the profile that submitted this request. */
    private Long profileId;

    /** Full name of the employee who submitted the request. */
    private String requesterName;

    /** Name of the leave type (e.g., "Annual Leave"). */
    private String leaveTypeName;

    /** First day of the leave period. */
    private LocalDate startDate;

    /** Last day of the leave period. */
    private LocalDate endDate;

    /** Current status of the request (PENDING, APPROVED, REJECTED, CANCELLED). */
    private LeaveStatus status;

    /** Employee-provided reason for the request. */
    private String reason;

    /** Timestamp when the request was first submitted. */
    private LocalDateTime createdAt;
}
