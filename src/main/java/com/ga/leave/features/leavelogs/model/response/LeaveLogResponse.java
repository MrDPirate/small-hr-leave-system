package com.ga.leave.features.leavelogs.model.response;

import com.ga.leave.features.leavelogs.model.LeaveAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a leave log entry.
 * Provides an audit trail of actions taken on a leave request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveLogResponse {

    /** Unique identifier of the log entry. */
    private Long id;

    /** ID of the leave request this log entry belongs to. */
    private Long requestId;

    /** Full name of the user who performed the action. */
    private String actorName;

    /** The action that was taken on the request. */
    private LeaveAction action;

    /** Human-readable description, e.g., rejection reason. */
    private String description;

    /** Timestamp when this log entry was recorded. */
    private LocalDateTime createdAt;
}
