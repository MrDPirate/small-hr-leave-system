package com.ga.leave.features.leaverequest.service;

import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.service.LeaveLogService;
import com.ga.leave.features.leaverequest.model.LeavePeriod;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.LeaveStatus;
import com.ga.leave.features.leaverequest.model.request.RejectLeaveRequest;
import com.ga.leave.features.leaverequest.model.request.SubmitLeaveRequest;
import com.ga.leave.features.leaverequest.model.response.LeaveRequestResponse;
import com.ga.leave.features.leaverequest.repository.LeaveRequestRepository;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.repository.LeaveTypeRepository;
import com.ga.leave.mapper.LeaveMapper;
import com.ga.leave.model.Role;
import com.ga.leave.model.RoleName;
import com.ga.leave.model.User;
import com.ga.leave.model.UserProfile;
import com.ga.leave.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeaveRequestService}.
 * Verifies the leave request workflow including submission, approval, rejection, and authorization.
 */
@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveLogService leaveLogService;
    @Mock private LeaveMapper leaveMapper;
    @Mock private UserContextService userContextService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private UserProfile employeeProfile;
    private UserProfile managerProfile;
    private User employeeUser;
    private User managerUser;
    private LeaveType annualLeave;
    private LeaveRequest pendingRequest;
    private LeaveRequestResponse sampleResponse;

    /**
     * Sets up shared test fixtures before each test.
     * Employee profile is managed by the manager profile.
     */
    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setName(RoleName.ROLE_USER);

        Role adminRole = new Role();
        adminRole.setName(RoleName.ROLE_ADMIN);

        managerProfile = new UserProfile();
        managerProfile.setId(1L);
        managerProfile.setFirstName("Khalil");
        managerProfile.setLastName("Admin");

        managerUser = new User();
        managerUser.setId(1L);
        managerUser.setUserProfile(managerProfile);
        managerUser.setRoles(Set.of(adminRole, userRole));

        employeeProfile = new UserProfile();
        employeeProfile.setId(2L);
        employeeProfile.setFirstName("Hasan");
        employeeProfile.setLastName("User");
        employeeProfile.setManager(managerProfile);

        employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setUserProfile(employeeProfile);
        employeeUser.setRoles(Set.of(userRole));

        annualLeave = new LeaveType(1L, "Annual Leave", 20);

        pendingRequest = new LeaveRequest();
        pendingRequest.setId(10L);
        pendingRequest.setProfile(employeeProfile);
        pendingRequest.setLeaveType(annualLeave);
        pendingRequest.setLeavePeriod(new LeavePeriod(
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(14)));
        pendingRequest.setStatus(LeaveStatus.PENDING);
        pendingRequest.setReason("Vacation");
        pendingRequest.setCreatedAt(LocalDateTime.now());

        sampleResponse = new LeaveRequestResponse(10L, 2L, "Hasan User",
                "Annual Leave", LocalDate.now().plusDays(7), LocalDate.now().plusDays(14),
                LeaveStatus.PENDING, "Vacation", LocalDateTime.now());
    }

    /**
     * Tests that submitting a valid leave request creates and saves a PENDING request with a log entry.
     */
    @Test
    void submitRequest_validInput_createsAndReturnsResponse() {
        SubmitLeaveRequest request = new SubmitLeaveRequest(
                1L, LocalDate.now().plusDays(7), LocalDate.now().plusDays(14), "Vacation");

        when(userContextService.getCurrentProfile()).thenReturn(employeeProfile);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);
        when(leaveMapper.toLeaveRequestResponse(pendingRequest)).thenReturn(sampleResponse);

        LeaveRequestResponse result = leaveRequestService.submitRequest(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verify(leaveLogService).log(eq(pendingRequest), eq(employeeProfile), eq(LeaveAction.SUBMITTED), anyString());
    }

    /**
     * Tests that submitting with an invalid date range (end before start) throws IllegalArgumentException.
     */
    @Test
    void submitRequest_endBeforeStart_throwsIllegalArgumentException() {
        SubmitLeaveRequest request = new SubmitLeaveRequest(
                1L, LocalDate.now().plusDays(14), LocalDate.now().plusDays(7), "Bad dates");

        when(userContextService.getCurrentProfile()).thenReturn(employeeProfile);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));

        assertThatThrownBy(() -> leaveRequestService.submitRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must not be before start date");
    }

    /**
     * Tests that the assigned manager can successfully approve a PENDING request.
     */
    @Test
    void approveRequest_byAssignedManager_approvesRequest() {
        when(userContextService.getCurrentProfile()).thenReturn(managerProfile);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        LeaveRequestResponse approvedResponse = new LeaveRequestResponse(10L, 2L, "Hasan User",
                "Annual Leave", LocalDate.now().plusDays(7), LocalDate.now().plusDays(14),
                LeaveStatus.APPROVED, "Vacation", LocalDateTime.now());
        when(leaveMapper.toLeaveRequestResponse(pendingRequest)).thenReturn(approvedResponse);

        LeaveRequestResponse result = leaveRequestService.approveRequest(10L);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        verify(leaveLogService).log(eq(pendingRequest), eq(managerProfile), eq(LeaveAction.APPROVED), anyString());
    }

    /**
     * Tests that a non-manager, non-admin user cannot approve a request.
     */
    @Test
    void approveRequest_byUnauthorizedUser_throwsAccessDeniedException() {
        UserProfile otherProfile = new UserProfile();
        otherProfile.setId(99L);

        Role userRole = new Role();
        userRole.setName(RoleName.ROLE_USER);
        User otherUser = new User();
        otherUser.setRoles(Set.of(userRole));

        when(userContextService.getCurrentProfile()).thenReturn(otherProfile);
        when(userContextService.getCurrentUser()).thenReturn(otherUser);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.approveRequest(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Tests that approving an already-approved request throws IllegalStateException.
     */
    @Test
    void approveRequest_alreadyApproved_throwsIllegalStateException() {
        pendingRequest.setStatus(LeaveStatus.APPROVED);

        when(userContextService.getCurrentProfile()).thenReturn(managerProfile);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.approveRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    /**
     * Tests that an employee can cancel their own PENDING request.
     */
    @Test
    void cancelRequest_ownPendingRequest_cancelsSuccessfully() {
        when(userContextService.getCurrentProfile()).thenReturn(employeeProfile);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        LeaveRequestResponse cancelledResponse = new LeaveRequestResponse(10L, 2L, "Hasan User",
                "Annual Leave", LocalDate.now().plusDays(7), LocalDate.now().plusDays(14),
                LeaveStatus.CANCELLED, "Vacation", LocalDateTime.now());
        when(leaveMapper.toLeaveRequestResponse(pendingRequest)).thenReturn(cancelledResponse);

        LeaveRequestResponse result = leaveRequestService.cancelRequest(10L);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        verify(leaveLogService).log(eq(pendingRequest), eq(employeeProfile), eq(LeaveAction.CANCELLED), anyString());
    }

    /**
     * Tests that an employee cannot cancel someone else's request.
     */
    @Test
    void cancelRequest_notOwner_throwsAccessDeniedException() {
        UserProfile otherProfile = new UserProfile();
        otherProfile.setId(99L);

        when(userContextService.getCurrentProfile()).thenReturn(otherProfile);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> leaveRequestService.cancelRequest(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Tests that the assigned manager can reject a PENDING request with a reason.
     */
    @Test
    void rejectRequest_byAssignedManager_rejectsRequest() {
        RejectLeaveRequest rejectReq = new RejectLeaveRequest("Too many people on leave");

        when(userContextService.getCurrentProfile()).thenReturn(managerProfile);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(pendingRequest)).thenReturn(pendingRequest);

        LeaveRequestResponse rejectedResponse = new LeaveRequestResponse(10L, 2L, "Hasan User",
                "Annual Leave", LocalDate.now().plusDays(7), LocalDate.now().plusDays(14),
                LeaveStatus.REJECTED, "Vacation", LocalDateTime.now());
        when(leaveMapper.toLeaveRequestResponse(pendingRequest)).thenReturn(rejectedResponse);

        LeaveRequestResponse result = leaveRequestService.rejectRequest(10L, rejectReq);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        verify(leaveLogService).log(eq(pendingRequest), eq(managerProfile), eq(LeaveAction.REJECTED), anyString());
    }
}
