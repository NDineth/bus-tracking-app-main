package com.bustrackpro.dto.response.tracking;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for sending tracking analysis responses.
 */
@Data
@Builder
public class TrackingResponse {

    private Integer busId;

    private String routeNumber;

    private String tripCode;

    /** Null when no active trip is found for the bus. */
    private Boolean onRoute;

    private Double deviationMeters;

    private String nearestHaltName;

    private List<String> skippedHalts;

    private List<ViolationDetail> violations;

    private String timestamp;
}
