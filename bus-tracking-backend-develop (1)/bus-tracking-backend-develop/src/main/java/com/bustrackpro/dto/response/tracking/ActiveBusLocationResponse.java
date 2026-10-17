package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for sending real-time active bus locations to the frontend map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveBusLocationResponse {
    private Integer busId;
    private String registrationNumber;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private LocalDateTime recordedAt;
    private Long routeId;
    private String routeNumber;
}
