package com.bustrackpro.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending trip responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Integer id;
    private Integer timetableId;
    private String timetableName;
    private String departureTime;
    private String arrivalTime;
    private Integer bufferTimeMinutes;
    private Integer estimatedDurationMinutes;
    private String tripCode;
    private String tripLabel;
    private String direction;
    private String status;
}
