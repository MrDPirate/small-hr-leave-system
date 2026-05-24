package com.ga.leave.features.leavetype.repository;

import com.ga.leave.features.leavetype.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link LeaveType} persistence.
 * Managed by Spring Data JPA; all CRUD methods are provided automatically.
 */
@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    /**
     * Checks whether a leave type with the given name already exists.
     *
     * @param name the leave type name to check
     * @return true if a record with this name exists
     */
    boolean existsByName(String name);

    /**
     * Finds a leave type by its display name.
     *
     * @param name the name to search for
     * @return an Optional containing the matching LeaveType, or empty
     */
    Optional<LeaveType> findByName(String name);
}
