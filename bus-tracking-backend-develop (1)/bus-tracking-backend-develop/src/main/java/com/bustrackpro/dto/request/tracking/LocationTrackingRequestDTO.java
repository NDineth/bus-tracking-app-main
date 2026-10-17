package com.bustrackpro.dto.request.tracking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for location tracking request.
 */
public record LocationTrackingRequestDTO(
        @NotNull(message = "Route ID is required")
        Integer routeId,

        @NotNull(message = "Latitude is required")
        @Min(value = -90, message = "Latitude must be between -90 and 90")
        @Max(value = 90, message = "Latitude must be between -90 and 90")
        Double currentLatitude,

        @NotNull(message = "Longitude is required")
        @Min(value = -180, message = "Longitude must be between -180 and 180")
        @Max(value = 180, message = "Longitude must be between -180 and 180")
        Double currentLongitude
) {
}
