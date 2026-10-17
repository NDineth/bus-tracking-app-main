package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for violation records returned to the frontend violations table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationResponse {
    private Integer id;
    private Integer busId;
    private String registrationNumber;
    private String routeNumber;
    private String type;
    private String severity;
    private String message;
    private Double latitude;
    private Double longitude;
    private LocalDateTime detectedAt;
    private LocalDateTime timestamp;
}
