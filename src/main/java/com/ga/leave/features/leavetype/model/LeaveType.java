package com.ga.leave.features.leavetype.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * Entity representing a category of leave an employee can request.
 * Examples: Annual Leave (20 days), Sick Leave (10 days).
 * HR admins (ROLE_ADMIN) manage these records.
 */
@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LeaveType {

    /** Primary key, auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique display name for this leave category (e.g., "Annual Leave"). */
    @Column(nullable = false, unique = true)
    @NotBlank
    private String name;

    /**
     * Default number of days allocated per year for this leave type.
     * Must be a positive integer.
     */
    @Column(nullable = false)
    @Positive
    private int defaultDays;
}
