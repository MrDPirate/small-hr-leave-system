package com.ga.leave.features.leavelogs.controller;

import com.ga.leave.config.TestSecurityConfig;
import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.model.response.LeaveLogResponse;
import com.ga.leave.features.leavelogs.service.LeaveLogService;
import com.ga.leave.security.JWTUtils;
import com.ga.leave.security.MyUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for {@link LeaveLogController}.
 * Verifies HTTP access control and response structure with a mocked service.
 */
@WebMvcTest(LeaveLogController.class)
@Import(TestSecurityConfig.class)
class LeaveLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveLogService leaveLogService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    @MockitoBean
    private JWTUtils jwtUtils;

    private LeaveLogResponse buildSampleLog() {
        return new LeaveLogResponse(1L, 10L, "Khalil Admin",
                LeaveAction.APPROVED, "Approved", LocalDateTime.now());
    }

    /**
     * Tests that an HR admin can access the full list of leave logs.
     * Expected: 200 OK with an array in the data field.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllLogs_adminUser_returns200() throws Exception {
        when(leaveLogService.getAllLogs()).thenReturn(List.of(buildSampleLog()));

        mockMvc.perform(get("/api/v1/leave-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    /**
     * Tests that a regular user is forbidden from accessing the full log list.
     * Expected: 403 Forbidden.
     */
    @Test
    @WithMockUser(roles = "USER")
    void getAllLogs_regularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/leave-logs"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that an authenticated user can access logs for a specific leave request.
     * Service-level authorization is not tested here — only the HTTP layer.
     * Expected: 200 OK.
     */
    @Test
    @WithMockUser
    void getLogsForRequest_authenticatedUser_returns200() throws Exception {
        when(leaveLogService.getLogsForRequest(10L)).thenReturn(List.of(buildSampleLog()));

        mockMvc.perform(get("/api/v1/leave-logs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].action").value("APPROVED"));
    }
}
