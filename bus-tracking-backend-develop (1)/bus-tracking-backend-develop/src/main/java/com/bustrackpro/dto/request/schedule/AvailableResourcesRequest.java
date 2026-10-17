package com.bustrackpro.dto.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for requesting available buses and drivers for a time slot.
 */
@Data
public class AvailableResourcesRequest {

    @NotNull
    private String date;

    @NotNull
    private String departureTime;

    @NotNull
    private String arrivalTime;
}
