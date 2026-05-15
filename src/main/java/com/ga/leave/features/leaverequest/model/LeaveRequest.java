package com.ga.leave.features.leaverequest.model;

import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.model.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Aggregate root for the leave request workflow.
 * An employee submits a request; their assigned manager approves or rejects it.
 * Business rules are enforced by the domain methods {@link #approve()},
 * {@link #reject()}, and {@link #cancel()}.
 */
@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"profile", "leaveType"})
public class LeaveRequest {

    /** Primary key, auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The employee who submitted this leave request.
     * Must not be null; fetched lazily to avoid unnecessary joins.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    /**
     * The category of leave being requested (e.g., Annual, Sick).
     * Must not be null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /**
     * The date range of the leave period.
     * Stored as embedded columns (start_date, end_date) in this table.
     */
    @Embedded
    private LeavePeriod leavePeriod;

    /**
     * Current lifecycle state of this request.
     * Starts as {@link LeaveStatus#PENDING} on creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    /** Employee-provided reason or notes for the leave request. */
    @Column(length = 500)
    private String reason;

    /** Timestamp when the request was first created. Immutable. */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Timestamp of the last update to this request. */
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Domain business methods
    // -------------------------------------------------------------------------

    /**
     * Transitions this request to {@link LeaveStatus#APPROVED}.
     * Only valid when the current status is {@link LeaveStatus#PENDING}.
     *
     * @throws IllegalStateException if the request is not in PENDING state
     */
    public void approve() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve a request that is not PENDING. Current status: " + this.status);
        }
        this.status = LeaveStatus.APPROVED;
    }

    /**
     * Transitions this request to {@link LeaveStatus#REJECTED}.
     * Only valid when the current status is {@link LeaveStatus#PENDING}.
     *
     * @throws IllegalStateException if the request is not in PENDING state
     */
    public void reject() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject a request that is not PENDING. Current status: " + this.status);
        }
        this.status = LeaveStatus.REJECTED;
    }

    /**
     * Transitions this request to {@link LeaveStatus#CANCELLED}.
     * Only valid when the current status is {@link LeaveStatus#PENDING}.
     *
     * @throws IllegalStateException if the request is not in PENDING state
     */
    public void cancel() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot cancel a request that is not PENDING. Current status: " + this.status);
        }
        this.status = LeaveStatus.CANCELLED;
    }
}
