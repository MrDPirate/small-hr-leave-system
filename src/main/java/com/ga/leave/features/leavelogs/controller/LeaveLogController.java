package com.ga.leave.features.leavelogs.controller;

import com.ga.leave.features.leavelogs.model.response.LeaveLogResponse;
import com.ga.leave.features.leavelogs.service.LeaveLogService;
import com.ga.leave.model.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for querying leave audit logs.
 * HR admins can view all logs; employees and managers can view logs for specific requests
 * they are authorized to access.
 *
 * <p>Base path: {@code /api/v1/leave-logs}</p>
 */
@RestController
@RequestMapping("/api/v1/leave-logs")
@RequiredArgsConstructor
@Tag(name = "Leave Logs", description = "Audit trail for leave request actions")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveLogController {

    private final LeaveLogService leaveLogService;

    /**
     * Returns all audit log entries in the system.
     * Restricted to HR admins (ROLE_ADMIN).
     *
     * @return 200 with the complete list of log entry DTOs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all leave logs", description = "HR admin only — returns the full audit trail")
    public ResponseEntity<ApiResponse<List<LeaveLogResponse>>> getAllLogs() {
        List<LeaveLogResponse> logs = leaveLogService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Returns all audit log entries for a specific leave request.
     * Access is granted to the request owner, their assigned manager, or an HR admin.
     * The authorization check is performed in the service layer.
     *
     * @param requestId the ID of the leave request whose logs are requested
     * @return 200 with the ordered list of log entries (oldest first)
     */
    @GetMapping("/{requestId}")
    @Operation(summary = "Get logs for a request",
            description = "Returns audit log entries for the given leave request. " +
                    "Accessible to the request owner, their manager, or HR admin.")
    public ResponseEntity<ApiResponse<List<LeaveLogResponse>>> getLogsForRequest(
            @PathVariable Long requestId) {
        List<LeaveLogResponse> logs = leaveLogService.getLogsForRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
