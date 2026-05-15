package com.ga.leave.features.leavetype.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a leave type.
 * Returned to clients for read operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeResponse {

    /** Unique identifier of the leave type. */
    private Long id;

    /** Display name of the leave category. */
    private String name;

    /** Default number of days allocated per year. */
    private int defaultDays;
}
