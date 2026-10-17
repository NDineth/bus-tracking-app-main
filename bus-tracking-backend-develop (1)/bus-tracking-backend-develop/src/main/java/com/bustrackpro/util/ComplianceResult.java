package com.bustrackpro.util;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Result of a route compliance analysis.
 * Returned by {@link RouteComplianceUtil#analyseCompliance}.
 */
@Data
@Builder
public class ComplianceResult {

    /** True if the bus is currently deviating from the route corridor. */
    private boolean routeDeviation;

    /** Minimum perpendicular distance (meters) from the bus to the route. */
    private double minDistanceToRouteMeters;

    /** Names of halts that were skipped (bus passed them without stopping). */
    @Builder.Default
    private List<String> skippedHaltNames = new ArrayList<>();

    /** Stop order of the nearest halt to the current GPS position. */
    private Integer nearestHaltOrder;

    /** Name of the nearest halt. */
    private String nearestHaltName;
}
