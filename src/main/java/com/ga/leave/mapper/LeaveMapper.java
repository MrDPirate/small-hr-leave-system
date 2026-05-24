package com.ga.leave.mapper;

import com.ga.leave.features.leavelogs.model.LeaveLog;
import com.ga.leave.features.leavelogs.model.response.LeaveLogResponse;
import com.ga.leave.features.leaverequest.model.LeaveRequest;
import com.ga.leave.features.leaverequest.model.response.LeaveRequestResponse;
import com.ga.leave.features.leavetype.model.LeaveType;
import com.ga.leave.features.leavetype.model.response.LeaveTypeResponse;
import com.ga.leave.model.UserProfile;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting leave-related entities to response DTOs.
 * Avoids lazy-loading issues by expecting callers to have already fetched
 * required associations before calling these methods.
 */
@Component
public class LeaveMapper {

    /**
     * Converts a {@link LeaveType} entity to a {@link LeaveTypeResponse} DTO.
     *
     * @param leaveType the entity to convert; must not be null
     * @return the corresponding response DTO
     */
    public LeaveTypeResponse toLeaveTypeResponse(LeaveType leaveType) {
        return new LeaveTypeResponse(
                leaveType.getId(),
                leaveType.getName(),
                leaveType.getDefaultDays()
        );
    }

    /**
     * Converts a {@link LeaveRequest} entity to a {@link LeaveRequestResponse} DTO.
     * Reads the requester's first and last name from the associated profile.
     *
     * @param request the entity to convert; must not be null
     * @return the corresponding response DTO
     */
    public LeaveRequestResponse toLeaveRequestResponse(LeaveRequest request) {
        UserProfile profile = request.getProfile();
        String requesterName = (profile.getFirstName() != null ? profile.getFirstName() : "")
                + " " + (profile.getLastName() != null ? profile.getLastName() : "");

        return new LeaveRequestResponse(
                request.getId(),
                profile.getId(),
                requesterName.trim(),
                request.getLeaveType().getName(),
                request.getLeavePeriod().startDate(),
                request.getLeavePeriod().endDate(),
                request.getStatus(),
                request.getReason(),
                request.getCreatedAt()
        );
    }

    /**
     * Converts a {@link LeaveLog} entity to a {@link LeaveLogResponse} DTO.
     * Reads the actor's name from the associated profile.
     *
     * @param log the entity to convert; must not be null
     * @return the corresponding response DTO
     */
    public LeaveLogResponse toLeaveLogResponse(LeaveLog log) {
        UserProfile actor = log.getActor();
        String actorName = (actor.getFirstName() != null ? actor.getFirstName() : "")
                + " " + (actor.getLastName() != null ? actor.getLastName() : "");

        return new LeaveLogResponse(
                log.getId(),
                log.getRequest().getId(),
                actorName.trim(),
                log.getAction(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}
