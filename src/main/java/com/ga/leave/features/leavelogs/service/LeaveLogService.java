package com.ga.leave.features.leavelogs.service;

import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.model.LeaveLog;
import com.ga.leave.features.leavelogs.model.response.LeaveLogResponse;
import com.ga.leave.features.leavelogs.repository.LeaveLogRepository;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.repository.LeaveRequestRepository;
import com.ga.leave.mapper.LeaveMapper;
import com.ga.leave.model.UserProfile;
import com.ga.leave.model.RoleName;
import com.ga.leave.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for creating and querying leave audit log entries.
 * Log entries are immutable once created — they are never updated or deleted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveLogService {

    private final LeaveLogRepository leaveLogRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveMapper leaveMapper;
    private final UserContextService userContextService;

    /**
     * Records an audit log entry for an action performed on a leave request.
     * Called internally by {@code LeaveRequestService}; not exposed as an API.
     *
     * @param request     the leave request that was acted upon
     * @param actor       the user profile that performed the action
     * @param action      the type of action taken
     * @param description optional human-readable description (e.g., rejection reason)
     */
    @Transactional
    public void log(LeaveRequest request, UserProfile actor, LeaveAction action, String description) {
        LeaveLog logEntry = new LeaveLog();
        logEntry.setRequest(request);
        logEntry.setActor(actor);
        logEntry.setAction(action);
        logEntry.setDescription(description);
        leaveLogRepository.save(logEntry);
        log.debug("Logged action {} on request id={} by actor id={}", action, request.getId(), actor.getId());
    }

    /**
     * Returns all log entries in the system.
     * Restricted to HR admins (ROLE_ADMIN) via {@code @PreAuthorize} on the controller.
     *
     * @return list of all leave log response DTOs
     */
    public List<LeaveLogResponse> getAllLogs() {
        return leaveLogRepository.findAll()
                .stream()
                .map(leaveMapper::toLeaveLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all log entries for a specific leave request.
     * Access is granted to the request owner, their manager, or an HR admin.
     *
     * @param requestId the ID of the leave request whose logs are requested
     * @return ordered list of log entries (oldest first)
     * @throws InformationNotFoundException if the request does not exist
     * @throws AccessDeniedException        if the caller is not authorized
     */
    public List<LeaveLogResponse> getLogsForRequest(Long requestId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + requestId + " not found"));

        UserProfile currentProfile = userContextService.getCurrentProfile();
        boolean isOwner = request.getProfile().getId().equals(currentProfile.getId());
        boolean isManager = request.getProfile().getManager() != null
                && request.getProfile().getManager().getId().equals(currentProfile.getId());
        boolean isAdmin = userContextService.getCurrentUser().getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!isOwner && !isManager && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view logs for this request");
        }

        return leaveLogRepository.findByRequest_IdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(leaveMapper::toLeaveLogResponse)
                .collect(Collectors.toList());
    }
}
