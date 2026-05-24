package com.ga.leave.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "manager"})
public class UserProfile {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String firstName;
    @Column
    private String lastName;

    @Column
    private Long phoneNumber;

    private String imageName;
    private String imageType;

    @Column(name = "image_data", columnDefinition = "BYTEA")
    private byte[] imageData;

//    will be used if we decide to store images in the filesystem or a cloud storage service instead of the database
//    @Column
//    private String imageUrl;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "userProfile", fetch = FetchType.LAZY)
    private User user;

    /**
     * The manager responsible for approving this employee's leave requests.
     * Self-referencing relationship — any profile can be another profile's manager.
     * Nullable: employees without an assigned manager cannot have their requests approved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private UserProfile manager;

}