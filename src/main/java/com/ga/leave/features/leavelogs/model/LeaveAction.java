package com.ga.leave.features.leavelogs.model;

/**
 * Actions that can be recorded in the leave audit log.
 *
 * <ul>
 *   <li>{@link #SUBMITTED}  — employee submitted a new leave request</li>
 *   <li>{@link #APPROVED}   — manager approved the request</li>
 *   <li>{@link #REJECTED}   — manager rejected the request</li>
 *   <li>{@link #UPDATED}    — employee updated a pending request</li>
 *   <li>{@link #CANCELLED}  — employee cancelled a pending request</li>
 * </ul>
 */
public enum LeaveAction {
    SUBMITTED,
    APPROVED,
    REJECTED,
    UPDATED,
    CANCELLED
}
