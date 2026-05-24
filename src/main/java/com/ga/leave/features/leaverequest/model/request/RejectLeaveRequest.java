package com.ga.leave.features.leaverequest.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for rejecting a leave request.
 * The manager must supply a reason for the rejection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectLeaveRequest {

    /** Mandatory reason explaining why the request was rejected. */
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
