package com.ga.leave.features.leavetype.service;

import com.ga.leave.exception.InformationExistException;
import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.model.request.LeaveTypeRequest;
import com.ga.leave.features.leavetype.model.response.LeaveTypeResponse;
import com.ga.leave.features.leavetype.repository.LeaveTypeRepository;
import com.ga.leave.mapper.LeaveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeaveTypeService}.
 * Uses Mockito to isolate business logic from the database layer.
 */
@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveMapper leaveMapper;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    private LeaveType sampleLeaveType;
    private LeaveTypeRequest sampleRequest;
    private LeaveTypeResponse sampleResponse;

    /**
     * Sets up shared test fixtures before each test.
     */
    @BeforeEach
    void setUp() {
        sampleLeaveType = new LeaveType(1L, "Annual Leave", 20);
        sampleRequest = new LeaveTypeRequest("Annual Leave", 20);
        sampleResponse = new LeaveTypeResponse(1L, "Annual Leave", 20);
    }

    /**
     * Tests that a new leave type is saved and returned successfully.
     */
    @Test
    void createLeaveType_success_returnsResponse() {
        when(leaveTypeRepository.existsByName("Annual Leave")).thenReturn(false);
        when(leaveTypeRepository.save(any(LeaveType.class))).thenReturn(sampleLeaveType);
        when(leaveMapper.toLeaveTypeResponse(sampleLeaveType)).thenReturn(sampleResponse);

        LeaveTypeResponse result = leaveTypeService.createLeaveType(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Annual Leave");
        assertThat(result.getDefaultDays()).isEqualTo(20);
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    /**
     * Tests that creating a duplicate leave type name throws {@link InformationExistException}.
     */
    @Test
    void createLeaveType_duplicateName_throwsInformationExistException() {
        when(leaveTypeRepository.existsByName("Annual Leave")).thenReturn(true);

        assertThatThrownBy(() -> leaveTypeService.createLeaveType(sampleRequest))
                .isInstanceOf(InformationExistException.class)
                .hasMessageContaining("Annual Leave");

        verify(leaveTypeRepository, never()).save(any());
    }

    /**
     * Tests that all leave types are fetched and mapped correctly.
     */
    @Test
    void getAllLeaveTypes_returnsAllMappedItems() {
        when(leaveTypeRepository.findAll()).thenReturn(List.of(sampleLeaveType));
        when(leaveMapper.toLeaveTypeResponse(sampleLeaveType)).thenReturn(sampleResponse);

        List<LeaveTypeResponse> result = leaveTypeService.getAllLeaveTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Annual Leave");
    }

    /**
     * Tests that fetching a leave type by ID returns the correct DTO.
     */
    @Test
    void getLeaveTypeById_existingId_returnsResponse() {
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(sampleLeaveType));
        when(leaveMapper.toLeaveTypeResponse(sampleLeaveType)).thenReturn(sampleResponse);

        LeaveTypeResponse result = leaveTypeService.getLeaveTypeById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    /**
     * Tests that fetching a non-existent leave type throws {@link InformationNotFoundException}.
     */
    @Test
    void getLeaveTypeById_notFound_throwsInformationNotFoundException() {
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.getLeaveTypeById(99L))
                .isInstanceOf(InformationNotFoundException.class)
                .hasMessageContaining("99");
    }

    /**
     * Tests that updating an existing leave type saves and returns the updated record.
     */
    @Test
    void updateLeaveType_success_returnsUpdatedResponse() {
        LeaveTypeRequest updateReq = new LeaveTypeRequest("Annual Leave Updated", 25);
        LeaveTypeResponse updatedResp = new LeaveTypeResponse(1L, "Annual Leave Updated", 25);

        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(sampleLeaveType));
        when(leaveTypeRepository.existsByName("Annual Leave Updated")).thenReturn(false);
        when(leaveTypeRepository.save(sampleLeaveType)).thenReturn(sampleLeaveType);
        when(leaveMapper.toLeaveTypeResponse(sampleLeaveType)).thenReturn(updatedResp);

        LeaveTypeResponse result = leaveTypeService.updateLeaveType(1L, updateReq);

        assertThat(result.getName()).isEqualTo("Annual Leave Updated");
        assertThat(result.getDefaultDays()).isEqualTo(25);
    }

    /**
     * Tests that deleting an existing leave type removes it from the repository.
     */
    @Test
    void deleteLeaveType_success_deletesRecord() {
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(sampleLeaveType));

        leaveTypeService.deleteLeaveType(1L);

        verify(leaveTypeRepository).delete(sampleLeaveType);
    }

    /**
     * Tests that deleting a non-existent leave type throws {@link InformationNotFoundException}.
     */
    @Test
    void deleteLeaveType_notFound_throwsInformationNotFoundException() {
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveTypeService.deleteLeaveType(99L))
                .isInstanceOf(InformationNotFoundException.class);
    }
}
