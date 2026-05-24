package com.ga.leave.features.leaverequest.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request body for updating an existing pending leave request.
 * Only the request owner may update, and only while the status is PENDING.
 * All fields are optional; only non-null fields will be applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeaveRequest {

    /** New leave type ID to switch to. Null means no change. */
    private Long leaveTypeId;

    /** New start date. Null means no change. */
    private LocalDate startDate;

    /** New end date. Null means no change. */
    private LocalDate endDate;

    /** Updated reason or notes. Null means no change. */
    private String reason;
}
