package com.ga.leave.features.leaverequest.controller;

import com.ga.leave.features.leaverequest.model.request.RejectLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.SubmitLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.UpdateLeaveRequest;
import com.ga.leave.features.leaverequest.model.response.LeaveRequestResponse;
import com.ga.leave.features.leaverequest.service.LeaveRequestService;
import com.ga.leave.model.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the leave request workflow.
 * Employees submit and manage their own requests; managers approve or reject them.
 * HR admins have full visibility. Manager authorization is enforced at the service layer.
 *
 * <p>Base path: {@code /api/v1/leave-requests}</p>
 */
@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Employee leave submission and manager approval workflow")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    /**
     * Submits a new leave request on behalf of the currently authenticated employee.
     *
     * @param request the submission payload (leaveTypeId, startDate, endDate, reason)
     * @return 201 with the newly created leave request DTO
     */
    @PostMapping
    @Operation(summary = "Submit a leave request",
            description = "Creates a PENDING leave request for the authenticated employee")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> submitRequest(
            @Valid @RequestBody SubmitLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Leave request submitted successfully"));
    }

    /**
     * Returns leave requests. HR admins see all requests; regular employees see only their own.
     *
     * @return 200 with the relevant list of leave request DTOs
     */
    @GetMapping
    @Operation(summary = "Get leave requests",
            description = "HR admins receive all requests; employees receive only their own")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getRequests() {
        // Determine view based on role — checked inside service
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<LeaveRequestResponse> responses = isAdmin
                ? leaveRequestService.getAllRequests()
                : leaveRequestService.getMyRequests();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Returns all leave requests from employees whose assigned manager is the current user.
     * Any authenticated user with at least one direct report will see results.
     *
     * @return 200 with leave request DTOs for the current user's team
     */
    @GetMapping("/my-team")
    @Operation(summary = "Get team's leave requests",
            description = "Returns requests from employees whose profile.manager equals the current user")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getMyTeamRequests() {
        List<LeaveRequestResponse> responses = leaveRequestService.getMyTeamRequests();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Returns a single leave request by ID.
     * Access is granted to the request owner, their manager, or an HR admin.
     *
     * @param id the leave request ID
     * @return 200 with the matching leave request DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by ID",
            description = "Accessible to request owner, their manager, or HR admin")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getRequestById(@PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates a pending leave request.
     * Only the request owner may update; only PENDING requests may be modified.
     *
     * @param id      the ID of the leave request to update
     * @param request the fields to update (all optional)
     * @return 200 with the updated leave request DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a pending leave request",
            description = "Owner only; request must be in PENDING status")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> updateRequest(
            @PathVariable Long id,
            @RequestBody UpdateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.updateRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave request updated successfully"));
    }

    /**
     * Cancels a pending leave request.
     * Only the request owner may cancel; only PENDING requests may be cancelled.
     *
     * @param id the ID of the leave request to cancel
     * @return 200 with the cancelled leave request DTO
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a pending leave request",
            description = "Owner only; sets status to CANCELLED")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancelRequest(@PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.cancelRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave request cancelled successfully"));
    }

    /**
     * Approves a pending leave request.
     * The caller must be the employee's assigned manager or an HR admin.
     * Authorization is enforced in the service layer.
     *
     * @param id the ID of the leave request to approve
     * @return 200 with the approved leave request DTO
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a leave request",
            description = "Caller must be the employee's assigned manager or HR admin")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveRequest(@PathVariable Long id) {
        LeaveRequestResponse response = leaveRequestService.approveRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave request approved successfully"));
    }

    /**
     * Rejects a pending leave request with a mandatory reason.
     * The caller must be the employee's assigned manager or an HR admin.
     * Authorization is enforced in the service layer.
     *
     * @param id      the ID of the leave request to reject
     * @param request the rejection payload containing a mandatory reason
     * @return 200 with the rejected leave request DTO
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a leave request",
            description = "Caller must be the employee's assigned manager or HR admin; reason is mandatory")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody RejectLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.rejectRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave request rejected"));
    }
}
