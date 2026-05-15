package com.ga.leave.features.leaverequest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Value object representing the date range of a leave request.
 * Stored as embedded columns (start_date, end_date) in the leave_requests table.
 * Implemented as a JDK 17 record — immutable by design.
 */
@Embeddable
public record LeavePeriod(

        /** Inclusive first day of the leave period. */
        @Column(name = "start_date", nullable = false)
        LocalDate startDate,

        /** Inclusive last day of the leave period. Must be >= startDate. */
        @Column(name = "end_date", nullable = false)
        LocalDate endDate
) {
    /**
     * Compact canonical constructor that validates the date range.
     *
     * @throws IllegalArgumentException if endDate is before startDate
     */
    public LeavePeriod {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
    }
}
