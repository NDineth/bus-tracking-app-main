package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for GPS log entries returned to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsLogResponse {
    private Integer id;
    private Integer busId;
    private String registrationNumber;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private LocalDateTime recordedAt;
    private Boolean onRoute;
    private Integer scheduleAssignmentId;
}
