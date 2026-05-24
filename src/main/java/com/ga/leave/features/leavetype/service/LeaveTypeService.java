package com.ga.leave.features.leavetype.service;

import com.ga.leave.exception.InformationExistException;
import com.ga.leave.exception.InformationNotFoundException;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.model.request.LeaveTypeRequest;
import com.ga.leave.features.leavetype.model.response.LeaveTypeResponse;
import com.ga.leave.features.leavetype.repository.LeaveTypeRepository;
import com.ga.leave.mapper.LeaveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling CRUD operations for leave types.
 * Only HR admins (ROLE_ADMIN) may create, update, or delete; all authenticated users may read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveMapper leaveMapper;

    /**
     * Creates a new leave type.
     * Throws {@link InformationExistException} if a leave type with the same name already exists.
     *
     * @param request the creation request containing name and defaultDays
     * @return the saved leave type as a response DTO
     */
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        if (leaveTypeRepository.existsByName(request.getName())) {
            throw new InformationExistException("Leave type '" + request.getName() + "' already exists");
        }
        LeaveType leaveType = new LeaveType();
        leaveType.setName(request.getName());
        leaveType.setDefaultDays(request.getDefaultDays());
        LeaveType saved = leaveTypeRepository.save(leaveType);
        log.info("Created leave type: {}", saved.getName());
        return leaveMapper.toLeaveTypeResponse(saved);
    }

    /**
     * Returns all leave types in the system.
     *
     * @return list of all leave type response DTOs
     */
    public List<LeaveTypeResponse> getAllLeaveTypes() {
        return leaveTypeRepository.findAll()
                .stream()
                .map(leaveMapper::toLeaveTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single leave type by its ID.
     * Throws {@link InformationNotFoundException} if not found.
     *
     * @param id the leave type ID
     * @return the matching leave type response DTO
     */
    public LeaveTypeResponse getLeaveTypeById(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave type with id " + id + " not found"));
        return leaveMapper.toLeaveTypeResponse(leaveType);
    }

    /**
     * Updates an existing leave type.
     * Throws {@link InformationNotFoundException} if the record does not exist.
     * Throws {@link InformationExistException} if the new name conflicts with another record.
     *
     * @param id      the ID of the leave type to update
     * @param request the updated name and/or defaultDays
     * @return the updated leave type response DTO
     */
    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave type with id " + id + " not found"));

        // Check for name conflict only if the name is actually changing
        if (!leaveType.getName().equalsIgnoreCase(request.getName())
                && leaveTypeRepository.existsByName(request.getName())) {
            throw new InformationExistException("Leave type '" + request.getName() + "' already exists");
        }

        leaveType.setName(request.getName());
        leaveType.setDefaultDays(request.getDefaultDays());
        LeaveType updated = leaveTypeRepository.save(leaveType);
        log.info("Updated leave type id={}: name={}, defaultDays={}", id, updated.getName(), updated.getDefaultDays());
        return leaveMapper.toLeaveTypeResponse(updated);
    }

    /**
     * Deletes a leave type by its ID.
     * Throws {@link InformationNotFoundException} if the record does not exist.
     *
     * @param id the ID of the leave type to delete
     */
    @Transactional
    public void deleteLeaveType(Long id) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException("Leave type with id " + id + " not found"));
        leaveTypeRepository.delete(leaveType);
        log.info("Deleted leave type id={}", id);
    }
}
