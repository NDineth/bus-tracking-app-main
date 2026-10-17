package com.bustrackpro.dto.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for receiving trip creation/update requests.
 */
@Data
public class TripRequest {

    @NotNull
    private Integer timetableId;

    @NotNull
    private String departureTime;

    @NotNull
    private String arrivalTime;

    private Integer bufferTimeMinutes;

    private Integer estimatedDurationMinutes;

    private String tripCode;

    private String tripLabel;

    private String direction;
}
