package com.ga.leave.features.leavelogs.model;

import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.model.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit log entry for any action taken on a {@link LeaveRequest}.
 * Records who did what and when. Once created, log entries are never modified.
 */
@Entity
@Table(name = "leave_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"request", "actor"})
public class LeaveLog {

    /** Primary key, auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The leave request this log entry is associated with.
     * Fetched lazily; never null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private LeaveRequest request;

    /**
     * The user profile that performed the action (employee or manager).
     * Fetched lazily; never null.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserProfile actor;

    /**
     * The type of action recorded in this log entry.
     * Stored as a string for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveAction action;

    /** Human-readable description of the action, e.g., the rejection reason. */
    @Column(length = 500)
    private String description;

    /**
     * Timestamp when this log entry was created.
     * Immutable — never updated after insert.
     */
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
