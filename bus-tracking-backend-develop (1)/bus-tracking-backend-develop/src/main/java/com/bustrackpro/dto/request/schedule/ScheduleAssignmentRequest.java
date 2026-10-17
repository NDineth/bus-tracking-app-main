package com.bustrackpro.dto.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for receiving schedule assignment creation requests.
 */
@Data
public class ScheduleAssignmentRequest {

    @NotNull
    private Integer tripId;

    @NotNull
    private Integer busId;

    private Integer driverId;

    @NotNull
    private String assignmentDate;

    private String notes;

    private Integer bufferTimeMinutes;
}
