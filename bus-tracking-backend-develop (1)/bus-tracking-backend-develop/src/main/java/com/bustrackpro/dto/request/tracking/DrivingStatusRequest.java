package com.bustrackpro.dto.request.tracking;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving driving status updates from IoT Hub.
 * Payload format: {"busId": 1, "status": "NORMAL", "recordedAt": "..."}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrivingStatusRequest {

    @NotNull(message = "Bus ID is required")
    private Integer busId;

    @NotNull(message = "Status is required")
    private String status;

    private Double latitude;

    private Double longitude;

    /** Client-side timestamp; server time used if absent. */
    private OffsetDateTime recordedAt;
}
