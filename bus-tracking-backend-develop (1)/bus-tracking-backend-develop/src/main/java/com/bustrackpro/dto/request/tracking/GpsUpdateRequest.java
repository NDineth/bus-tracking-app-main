package com.bustrackpro.dto.request.tracking;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

/**
 * DTO for receiving GPS update requests from a bus.
 */
@Data
public class GpsUpdateRequest {

    @NotNull(message = "Device Reference Number is required")
    private Integer deviceReferenceNumber;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    /** Optional speed in km/h. */
    private Double speed;

    /** Optional client-side timestamp; server time used if absent. */
    private OffsetDateTime recordedAt;

    /** Optional acceleration readings. */
    private List<AccelerationReadingDto> acceleration;
}
