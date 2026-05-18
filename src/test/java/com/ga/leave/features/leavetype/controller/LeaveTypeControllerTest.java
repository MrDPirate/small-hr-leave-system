package com.ga.leave.features.leavetype.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ga.leave.config.TestSecurityConfig;
import com.ga.leave.features.leavetype.model.request.LeaveTypeRequest;
import com.ga.leave.features.leavetype.model.response.LeaveTypeResponse;
import com.ga.leave.features.leavetype.service.LeaveTypeService;
import com.ga.leave.security.JWTUtils;
import com.ga.leave.security.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for {@link LeaveTypeController}.
 * Uses {@code @WebMvcTest} to load only the web layer with a mocked service.
 * Tests verify HTTP contract (status codes, response structure) rather than business logic.
 */
@WebMvcTest(LeaveTypeController.class)
@Import(TestSecurityConfig.class)
class LeaveTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private LeaveTypeService leaveTypeService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    @MockitoBean
    private JWTUtils jwtUtils;

    /**
     * Tests that any authenticated user can retrieve the list of leave types.
     * Expected: 200 OK with a success response body.
     */
    @Test
    @WithMockUser
    void getAllLeaveTypes_authenticatedUser_returns200() throws Exception {
        when(leaveTypeService.getAllLeaveTypes()).thenReturn(List.of(
                new LeaveTypeResponse(1L, "Annual Leave", 20),
                new LeaveTypeResponse(2L, "Sick Leave", 10)
        ));

        mockMvc.perform(get("/api/v1/leave-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    /**
     * Tests that an admin user can successfully create a leave type.
     * Expected: 201 Created with the new leave type in the response body.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void createLeaveType_adminUser_returns201() throws Exception {
        LeaveTypeRequest request = new LeaveTypeRequest("Annual Leave", 20);
        LeaveTypeResponse response = new LeaveTypeResponse(1L, "Annual Leave", 20);

        when(leaveTypeService.createLeaveType(any(LeaveTypeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/leave-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Annual Leave"));
    }

    /**
     * Tests that a regular user (ROLE_USER) is forbidden from creating a leave type.
     * Expected: 403 Forbidden (enforced by @PreAuthorize on the controller).
     */
    @Test
    @WithMockUser(roles = "USER")
    void createLeaveType_regularUser_returns403() throws Exception {
        LeaveTypeRequest request = new LeaveTypeRequest("Annual Leave", 20);

        mockMvc.perform(post("/api/v1/leave-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that an admin can delete a leave type.
     * Expected: 200 OK.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLeaveType_adminUser_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/leave-types/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
