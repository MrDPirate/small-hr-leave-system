package com.ga.leave.features.leaverequest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ga.leave.config.TestSecurityConfig;
import com.ga.leave.features.leaverequest.model.LeaveStatus;
import com.ga.leave.features.leaverequest.model.request.RejectLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.SubmitLeaveRequest;
import com.ga.leave.features.leaverequest.model.response.LeaveRequestResponse;
import com.ga.leave.features.leaverequest.service.LeaveRequestService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for {@link LeaveRequestController}.
 * Tests HTTP contract — status codes and response structure — with a mocked service.
 */
@WebMvcTest(LeaveRequestController.class)
@Import(TestSecurityConfig.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    @MockitoBean
    private JWTUtils jwtUtils;

    private LeaveRequestResponse buildSampleResponse(LeaveStatus status) {
        return new LeaveRequestResponse(
                10L, 2L, "Hasan User", "Annual Leave",
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(14),
                status, "Vacation", LocalDateTime.now());
    }

    /**
     * Tests that an authenticated user can submit a leave request.
     * Expected: 201 Created.
     */
    @Test
    @WithMockUser
    void submitRequest_authenticatedUser_returns201() throws Exception {
        SubmitLeaveRequest request = new SubmitLeaveRequest(
                1L, LocalDate.now().plusDays(7), LocalDate.now().plusDays(14), "Vacation");

        when(leaveRequestService.submitRequest(any(SubmitLeaveRequest.class)))
                .thenReturn(buildSampleResponse(LeaveStatus.PENDING));

        mockMvc.perform(post("/api/v1/leave-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    /**
     * Tests that GET /my-team returns 200 for an authenticated user.
     */
    @Test
    @WithMockUser
    void getMyTeamRequests_authenticatedUser_returns200() throws Exception {
        when(leaveRequestService.getMyTeamRequests())
                .thenReturn(List.of(buildSampleResponse(LeaveStatus.PENDING)));

        mockMvc.perform(get("/api/v1/leave-requests/my-team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Tests that an authenticated user can retrieve a specific leave request by ID.
     */
    @Test
    @WithMockUser
    void getRequestById_authenticatedUser_returns200() throws Exception {
        when(leaveRequestService.getRequestById(10L))
                .thenReturn(buildSampleResponse(LeaveStatus.PENDING));

        mockMvc.perform(get("/api/v1/leave-requests/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    /**
     * Tests that an authenticated user can approve a leave request.
     * (Service enforces manager/admin check; controller just passes through.)
     */
    @Test
    @WithMockUser
    void approveRequest_authenticatedUser_returns200() throws Exception {
        when(leaveRequestService.approveRequest(10L))
                .thenReturn(buildSampleResponse(LeaveStatus.APPROVED));

        mockMvc.perform(post("/api/v1/leave-requests/10/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    /**
     * Tests that an authenticated user can reject a leave request with a reason.
     */
    @Test
    @WithMockUser
    void rejectRequest_validReason_returns200() throws Exception {
        RejectLeaveRequest rejectRequest = new RejectLeaveRequest("Team at capacity");

        when(leaveRequestService.rejectRequest(eq(10L), any(RejectLeaveRequest.class)))
                .thenReturn(buildSampleResponse(LeaveStatus.REJECTED));

        mockMvc.perform(post("/api/v1/leave-requests/10/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    /**
     * Tests that cancelling a leave request returns 200 with CANCELLED status.
     */
    @Test
    @WithMockUser
    void cancelRequest_authenticatedUser_returns200() throws Exception {
        when(leaveRequestService.cancelRequest(10L))
                .thenReturn(buildSampleResponse(LeaveStatus.CANCELLED));

        mockMvc.perform(delete("/api/v1/leave-requests/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
