package com.ga.leave.features.leavetype.controller;

import com.ga.leave.features.leavetype.model.request.LeaveTypeRequest;
import com.ga.leave.features.leavetype.model.response.LeaveTypeResponse;
import com.ga.leave.features.leavetype.service.LeaveTypeService;
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
 * REST controller for leave type management.
 * HR admins (ROLE_ADMIN) can create, update, and delete leave types.
 * All authenticated users can read leave types.
 *
 * <p>Base path: {@code /api/v1/leave-types}</p>
 */
@RestController
@RequestMapping("/api/v1/leave-types")
@RequiredArgsConstructor
@Tag(name = "Leave Types", description = "HR admin endpoints for managing leave categories")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    /**
     * Returns all available leave types.
     * Accessible by all authenticated users.
     *
     * @return 200 with list of leave type DTOs
     */
    @GetMapping
    @Operation(summary = "Get all leave types", description = "Returns all leave categories available in the system")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getAllLeaveTypes() {
        List<LeaveTypeResponse> types = leaveTypeService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    /**
     * Returns a single leave type by its ID.
     * Accessible by all authenticated users.
     *
     * @param id the ID of the leave type
     * @return 200 with the matching leave type DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get leave type by ID")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> getLeaveTypeById(@PathVariable Long id) {
        LeaveTypeResponse response = leaveTypeService.getLeaveTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Creates a new leave type.
     * Restricted to HR admins (ROLE_ADMIN).
     *
     * @param request the leave type creation payload
     * @return 201 with the created leave type DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create leave type", description = "HR admin only — creates a new leave category")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> createLeaveType(
            @Valid @RequestBody LeaveTypeRequest request) {
        LeaveTypeResponse response = leaveTypeService.createLeaveType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Leave type created successfully"));
    }

    /**
     * Updates an existing leave type by ID.
     * Restricted to HR admins (ROLE_ADMIN).
     *
     * @param id      the ID of the leave type to update
     * @param request the updated fields
     * @return 200 with the updated leave type DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update leave type", description = "HR admin only — updates a leave category")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> updateLeaveType(
            @PathVariable Long id,
            @Valid @RequestBody LeaveTypeRequest request) {
        LeaveTypeResponse response = leaveTypeService.updateLeaveType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave type updated successfully"));
    }

    /**
     * Deletes a leave type by ID.
     * Restricted to HR admins (ROLE_ADMIN).
     *
     * @param id the ID of the leave type to delete
     * @return 200 with a success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete leave type", description = "HR admin only — removes a leave category")
    public ResponseEntity<ApiResponse<Void>> deleteLeaveType(@PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Leave type deleted successfully"));
    }
}
