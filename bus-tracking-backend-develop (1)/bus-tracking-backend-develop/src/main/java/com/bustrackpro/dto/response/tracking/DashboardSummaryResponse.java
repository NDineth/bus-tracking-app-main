package com.bustrackpro.dto.response.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the tracking dashboard summary cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private int activeBuses;
    private int totalFleetCount;
    private int unavailableBusesCount;
    private int onRouteBuses;
    private int activeViolations;
    private int tripsToday;
    private int completedTripsToday;
    private String activeBusesChange;
    private String onRouteChange;
    private String violationChange;
    private String tripsChange;
}
