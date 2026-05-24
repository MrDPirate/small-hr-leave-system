package com.ga.leave.features.leaverequest.repository;

import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.LeaveStatus;
import com.ga.leave.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link LeaveRequest} persistence.
 * Provides derived queries for employee and manager access patterns.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * Returns all leave requests submitted by the given employee profile.
     *
     * @param profile the employee's user profile
     * @return list of leave requests for this employee
     */
    List<LeaveRequest> findByProfile(UserProfile profile);

    /**
     * Returns all leave requests from employees whose assigned manager matches
     * the given profile. Used by managers to view their team's requests.
     *
     * @param manager the manager's user profile
     * @return list of leave requests from the manager's direct reports
     */
    List<LeaveRequest> findByProfile_Manager(UserProfile manager);

    /**
     * Returns leave requests for a specific employee filtered by status.
     *
     * @param profile the employee's user profile
     * @param status  the status to filter by
     * @return filtered list of leave requests
     */
    List<LeaveRequest> findByProfileAndStatus(UserProfile profile, LeaveStatus status);
}
