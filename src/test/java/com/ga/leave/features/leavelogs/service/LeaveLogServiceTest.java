package com.ga.leave.features.leavelogs.service;

import com.ga.leave.features.leavelogs.model.LeaveAction;
import com.ga.leave.features.leavelogs.model.LeaveLog;
import com.ga.leave.features.leavelogs.model.response.LeaveLogResponse;
import com.ga.leave.features.leavelogs.repository.LeaveLogRepository;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.LeaveStatus;
import com.ga.leave.features.leaverequest.repository.LeaveRequestRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeaveLogService}.
 * Verifies log creation, retrieval, and access control.
 */
@ExtendWith(MockitoExtension.class)
class LeaveLogServiceTest {

    @Mock private LeaveLogRepository leaveLogRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveMapper leaveMapper;
    @Mock private UserContextService userContextService;

    @InjectMocks
    private LeaveLogService leaveLogService;

    private UserProfile actorProfile;
    private LeaveRequest sampleRequest;

    /**
     * Sets up shared test fixtures.
     */
    @BeforeEach
    void setUp() {
        actorProfile = new UserProfile();
        actorProfile.setId(1L);
        actorProfile.setFirstName("Hasan");
        actorProfile.setLastName("User");

        sampleRequest = new LeaveRequest();
        sampleRequest.setId(10L);
        sampleRequest.setProfile(actorProfile);
        sampleRequest.setStatus(LeaveStatus.PENDING);
    }

    /**
     * Tests that calling log() saves a new LeaveLog entity with correct fields.
     */
    @Test
    void log_validInput_savesLogEntry() {
        leaveLogService.log(sampleRequest, actorProfile, LeaveAction.SUBMITTED, "Submitted");

        verify(leaveLogRepository).save(argThat(logEntry ->
                logEntry.getRequest().equals(sampleRequest) &&
                        logEntry.getActor().equals(actorProfile) &&
                        logEntry.getAction() == LeaveAction.SUBMITTED &&
                        "Submitted".equals(logEntry.getDescription())));
    }

    /**
     * Tests that getAllLogs() returns the full list of mapped log entries.
     */
    @Test
    void getAllLogs_returnsMappedList() {
        LeaveLog log = new LeaveLog();
        log.setRequest(sampleRequest);
        log.setActor(actorProfile);
        log.setAction(LeaveAction.SUBMITTED);
        log.setCreatedAt(LocalDateTime.now());

        LeaveLogResponse response = new LeaveLogResponse(1L, 10L, "Hasan User",
                LeaveAction.SUBMITTED, "Submitted", LocalDateTime.now());

        when(leaveLogRepository.findAll()).thenReturn(List.of(log));
        when(leaveMapper.toLeaveLogResponse(log)).thenReturn(response);

        List<LeaveLogResponse> result = leaveLogService.getAllLogs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo(LeaveAction.SUBMITTED);
    }

    /**
     * Tests that the request owner can access logs for their own request.
     */
    @Test
    void getLogsForRequest_ownerCanAccess_returnsLogs() {
        Role userRole = new Role();
        userRole.setName(RoleName.ROLE_USER);
        User ownerUser = new User();
        ownerUser.setRoles(Set.of(userRole));

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(sampleRequest));
        when(userContextService.getCurrentProfile()).thenReturn(actorProfile);
        when(userContextService.getCurrentUser()).thenReturn(ownerUser);
        when(leaveLogRepository.findByRequest_IdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        List<LeaveLogResponse> result = leaveLogService.getLogsForRequest(10L);

        assertThat(result).isEmpty();
        verify(leaveLogRepository).findByRequest_IdOrderByCreatedAtAsc(10L);
    }

    /**
     * Tests that an unauthorized user (not owner, not manager, not admin) cannot access logs.
     */
    @Test
    void getLogsForRequest_unauthorizedUser_throwsAccessDeniedException() {
        UserProfile outsider = new UserProfile();
        outsider.setId(99L);

        Role userRole = new Role();
        userRole.setName(RoleName.ROLE_USER);
        User outsiderUser = new User();
        outsiderUser.setRoles(Set.of(userRole));

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(sampleRequest));
        when(userContextService.getCurrentProfile()).thenReturn(outsider);
        when(userContextService.getCurrentUser()).thenReturn(outsiderUser);

        assertThatThrownBy(() -> leaveLogService.getLogsForRequest(10L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
