package com.ga.leave.features.leavelogs.repository;

import com.ga.leave.features.leavelogs.model.LeaveLog;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link LeaveLog} persistence.
 * Provides audit trail access for leave requests.
 */
@Repository
public interface LeaveLogRepository extends JpaRepository<LeaveLog, Long> {

    /**
     * Returns all log entries associated with the given leave request.
     *
     * @param request the leave request entity
     * @return ordered list of log entries (oldest first by creation time)
     */
    List<LeaveLog> findByRequestOrderByCreatedAtAsc(LeaveRequest request);

    /**
     * Returns all log entries for a leave request identified by its ID.
     * Avoids loading the full LeaveRequest entity when only the ID is available.
     *
     * @param requestId the ID of the leave request
     * @return ordered list of log entries for that request
     */
    List<LeaveLog> findByRequest_IdOrderByCreatedAtAsc(Long requestId);
}
