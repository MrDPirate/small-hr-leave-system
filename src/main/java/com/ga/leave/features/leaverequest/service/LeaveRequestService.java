package com.ga.leave.features.leaverequest.service;

import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.service.LeaveLogService;
import com.ga.leave.features.leaverequest.model.LeavePeriod;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.request.RejectLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.SubmitLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.UpdateLeaveRequest;
import com.ga.leave.features.leaverequest.model.response.LeaveRequestResponse;
import com.ga.leave.features.leaverequest.repository.LeaveRequestRepository;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.repository.LeaveTypeRepository;
import com.ga.leave.mapper.LeaveMapper;
import com.ga.leave.model.RoleName;
import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for the leave request workflow.
 * Handles submission, retrieval, updates, cancellation, and manager approval/rejection.
 * Authorization is enforced at the service layer for ownership and manager checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveLogService leaveLogService;
    private final LeaveMapper leaveMapper;
    private final UserContextService userContextService;

    /**
     * Submits a new leave request on behalf of the currently authenticated employee.
     * Validates that end date is not before start date, then creates a PENDING request
     * and logs a SUBMITTED action.
     *
     * @param request the submission details including leave type, dates, and reason
     * @return the newly created leave request as a response DTO
     * @throws InformationNotFoundException if the specified leave type does not exist
     * @throws IllegalArgumentException     if the date range is invalid
     */
    @Transactional
    public LeaveRequestResponse submitRequest(SubmitLeaveRequest request) {
        UserProfile profile = userContextService.getCurrentProfile();

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new InformationNotFoundException(
                        "Leave type with id " + request.getLeaveTypeId() + " not found"));

        // LeavePeriod record validates end >= start in its compact constructor
        LeavePeriod period = new LeavePeriod(request.getStartDate(), request.getEndDate());

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setProfile(profile);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setLeavePeriod(period);
        leaveRequest.setReason(request.getReason());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        leaveLogService.log(saved, profile, LeaveAction.SUBMITTED,
                "Leave request submitted: " + leaveType.getName());
        log.info("Leave request submitted by profile id={} for type '{}'", profile.getId(), leaveType.getName());
        return leaveMapper.toLeaveRequestResponse(saved);
    }

    /**
     * Returns all leave requests for the currently authenticated employee.
     *
     * @return list of the current user's leave request DTOs
     */
    public List<LeaveRequestResponse> getMyRequests() {
        UserProfile profile = userContextService.getCurrentProfile();
        return leaveRequestRepository.findByProfile(profile)
                .stream()
                .map(leaveMapper::toLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all leave requests in the system.
     * Intended for HR admins; role enforcement is done at the controller level via {@code @PreAuthorize}.
     *
     * @return list of all leave request DTOs
     */
    public List<LeaveRequestResponse> getAllRequests() {
        return leaveRequestRepository.findAll()
                .stream()
                .map(leaveMapper::toLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all leave requests from employees whose assigned manager is the current user.
     * Available to any authenticated user who is set as a manager on other profiles.
     *
     * @return list of leave request DTOs for the current user's direct reports
     */
    public List<LeaveRequestResponse> getMyTeamRequests() {
        UserProfile profile = userContextService.getCurrentProfile();
        return leaveRequestRepository.findByProfile_Manager(profile)
                .stream()
                .map(leaveMapper::toLeaveRequestResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single leave request by ID.
     * Access is granted to the request owner, their assigned manager, or an HR admin.
     *
     * @param id the ID of the leave request
     * @return the matching leave request DTO
     * @throws InformationNotFoundException if the request does not exist
     * @throws AccessDeniedException        if the caller is not authorized
     */
    public LeaveRequestResponse getRequestById(Long id) {
        LeaveRequest request = findAndAuthorize(id);
        return leaveMapper.toLeaveRequestResponse(request);
    }

    /**
     * Updates a pending leave request owned by the current user.
     * Only non-null fields in the update request are applied.
     * Logs an UPDATED action on success.
     *
     * @param id            the ID of the leave request to update
     * @param updateRequest the fields to update
     * @return the updated leave request DTO
     * @throws InformationNotFoundException if the request or new leave type does not exist
     * @throws AccessDeniedException        if the caller does not own this request
     * @throws IllegalStateException        if the request is not in PENDING status
     */
    @Transactional
    public LeaveRequestResponse updateRequest(Long id, UpdateLeaveRequest updateRequest) {
        UserProfile profile = userContextService.getCurrentProfile();
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + id + " not found"));

        if (!request.getProfile().getId().equals(profile.getId())) {
            throw new AccessDeniedException("You can only update your own leave requests");
        }
        if (request.getStatus() != com.ga.leave.features.leaverequest.model.LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be updated. Current status: " + request.getStatus());
        }

        if (updateRequest.getLeaveTypeId() != null) {
            LeaveType leaveType = leaveTypeRepository.findById(updateRequest.getLeaveTypeId())
                    .orElseThrow(() -> new InformationNotFoundException(
                            "Leave type with id " + updateRequest.getLeaveTypeId() + " not found"));
            request.setLeaveType(leaveType);
        }

        // Apply date changes if provided; reconstruct LeavePeriod to preserve validation
        java.time.LocalDate newStart = updateRequest.getStartDate() != null
                ? updateRequest.getStartDate() : request.getLeavePeriod().startDate();
        java.time.LocalDate newEnd = updateRequest.getEndDate() != null
                ? updateRequest.getEndDate() : request.getLeavePeriod().endDate();
        request.setLeavePeriod(new LeavePeriod(newStart, newEnd));

        if (updateRequest.getReason() != null) {
            request.setReason(updateRequest.getReason());
        }

        LeaveRequest updated = leaveRequestRepository.save(request);
        leaveLogService.log(updated, profile, LeaveAction.UPDATED, "Leave request updated");
        return leaveMapper.toLeaveRequestResponse(updated);
    }

    /**
     * Cancels a pending leave request owned by the current user.
     * Delegates the status transition to {@link LeaveRequest#cancel()} which enforces PENDING state.
     *
     * @param id the ID of the leave request to cancel
     * @return the cancelled leave request DTO
     * @throws InformationNotFoundException if the request does not exist
     * @throws AccessDeniedException        if the caller does not own this request
     * @throws IllegalStateException        if the request is not in PENDING status
     */
    @Transactional
    public LeaveRequestResponse cancelRequest(Long id) {
        UserProfile profile = userContextService.getCurrentProfile();
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + id + " not found"));

        if (!request.getProfile().getId().equals(profile.getId())) {
            throw new AccessDeniedException("You can only cancel your own leave requests");
        }

        request.cancel(); // domain method enforces PENDING check
        leaveRequestRepository.save(request);
        leaveLogService.log(request, profile, LeaveAction.CANCELLED, "Leave request cancelled by employee");
        return leaveMapper.toLeaveRequestResponse(request);
    }

    /**
     * Approves a pending leave request.
     * The caller must be the employee's assigned manager or an HR admin.
     *
     * @param id the ID of the leave request to approve
     * @return the approved leave request DTO
     * @throws InformationNotFoundException if the request does not exist
     * @throws AccessDeniedException        if the caller is not the assigned manager or admin
     * @throws IllegalStateException        if the request is not in PENDING status
     */
    @Transactional
    public LeaveRequestResponse approveRequest(Long id) {
        UserProfile managerProfile = userContextService.getCurrentProfile();
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + id + " not found"));

        assertManagerOrAdmin(request, managerProfile);

        request.approve(); // domain method enforces PENDING check
        leaveRequestRepository.save(request);
        leaveLogService.log(request, managerProfile, LeaveAction.APPROVED, "Leave request approved");
        log.info("Leave request id={} approved by profile id={}", id, managerProfile.getId());
        return leaveMapper.toLeaveRequestResponse(request);
    }

    /**
     * Rejects a pending leave request with a mandatory reason.
     * The caller must be the employee's assigned manager or an HR admin.
     *
     * @param id            the ID of the leave request to reject
     * @param rejectRequest the rejection details including a mandatory reason
     * @return the rejected leave request DTO
     * @throws InformationNotFoundException if the request does not exist
     * @throws AccessDeniedException        if the caller is not the assigned manager or admin
     * @throws IllegalStateException        if the request is not in PENDING status
     */
    @Transactional
    public LeaveRequestResponse rejectRequest(Long id, RejectLeaveRequest rejectRequest) {
        UserProfile managerProfile = userContextService.getCurrentProfile();
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + id + " not found"));

        assertManagerOrAdmin(request, managerProfile);

        request.reject(); // domain method enforces PENDING check
        leaveRequestRepository.save(request);
        leaveLogService.log(request, managerProfile, LeaveAction.REJECTED,
                "Rejected: " + rejectRequest.getReason());
        log.info("Leave request id={} rejected by profile id={}", id, managerProfile.getId());
        return leaveMapper.toLeaveRequestResponse(request);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads a leave request and verifies that the current user is the owner,
     * their assigned manager, or an HR admin.
     *
     * @param id the leave request ID
     * @return the authorized leave request entity
     */
    private LeaveRequest findAndAuthorize(Long id) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave request with id " + id + " not found"));

        UserProfile currentProfile = userContextService.getCurrentProfile();
        boolean isOwner = request.getProfile().getId().equals(currentProfile.getId());
        boolean isManager = request.getProfile().getManager() != null
                && request.getProfile().getManager().getId().equals(currentProfile.getId());
        boolean isAdmin = isAdmin();

        if (!isOwner && !isManager && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this leave request");
        }
        return request;
    }

    /**
     * Asserts that the given manager profile is the assigned manager of the request's
     * employee, or that the current user has ROLE_ADMIN. Throws AccessDeniedException otherwise.
     *
     * @param request       the leave request being acted upon
     * @param managerProfile the profile of the user attempting the action
     */
    private void assertManagerOrAdmin(LeaveRequest request, UserProfile managerProfile) {
        boolean isAssignedManager = request.getProfile().getManager() != null
                && request.getProfile().getManager().getId().equals(managerProfile.getId());
        if (!isAssignedManager && !isAdmin()) {
            throw new AccessDeniedException(
                    "Only the employee's assigned manager or an HR admin can approve or reject this request");
        }
    }

    /**
     * Checks whether the currently authenticated user holds the ROLE_ADMIN authority.
     *
     * @return true if the current user is an HR admin
     */
    private boolean isAdmin() {
        User currentUser = userContextService.getCurrentUser();
        return currentUser != null && currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
    }
}
