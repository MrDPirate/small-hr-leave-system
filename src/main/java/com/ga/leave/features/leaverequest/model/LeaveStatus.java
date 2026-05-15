package com.ga.leave.features.leaverequest.model;

/**
 * Lifecycle states of a leave request.
 *
 * <ul>
 *   <li>{@link #PENDING}   — submitted, awaiting manager action</li>
 *   <li>{@link #APPROVED}  — approved by the assigned manager</li>
 *   <li>{@link #REJECTED}  — rejected by the assigned manager</li>
 *   <li>{@link #CANCELLED} — withdrawn by the employee before a decision</li>
 * </ul>
 */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
