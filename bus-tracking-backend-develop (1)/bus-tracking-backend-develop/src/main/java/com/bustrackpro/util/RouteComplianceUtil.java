package com.bustrackpro.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure utility class for route compliance analysis.
 * <p>
 * Detects two types of violations:
 * <ul>
 *   <li><b>Route deviation</b> — bus GPS position is too far from
 *       any route segment (line between consecutive halts).</li>
 *   <li><b>Halt skipping</b> — bus has passed a halt without coming
 *       close enough, while already being near a later halt.</li>
 * </ul>
 * <p>
 * All distance calculations use the <b>Haversine formula</b> for
 * accuracy on Earth's surface.
 */
public final class RouteComplianceUtil {

    /** Earth's mean radius in meters. */
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /** Default: bus is off-route if farther than this from any segment. */
    public static final double DEFAULT_ROUTE_DEVIATION_METERS = 500.0;

    /** Default: bus "visits" a halt if it comes within this distance. */
    public static final double DEFAULT_HALT_PROXIMITY_METERS = 150.0;

    private RouteComplianceUtil() {
        // Utility class — no instantiation.
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Analyse the compliance of a bus GPS position against its route.
     *
     * @param currentLat            current bus latitude
     * @param currentLng            current bus longitude
     * @param haltLatitudes         ordered halt latitudes (by stopOrder)
     * @param haltLongitudes        ordered halt longitudes
     * @param haltNames             ordered halt names
     * @param haltOrders            ordered halt stop-order values
     * @param prevLogLatitudes      previous GPS log latitudes (time-ordered)
     * @param prevLogLongitudes     previous GPS log longitudes
     * @param routeDeviationMeters  threshold for route deviation
     * @param haltProximityMeters   threshold for halt proximity
     * @return a {@link ComplianceResult} with deviation and skip info
     */
    public static ComplianceResult analyseCompliance(
            double currentLat, double currentLng,
            List<Double> haltLatitudes, List<Double> haltLongitudes,
            List<String> haltNames, List<Integer> haltOrders,
            List<Double> prevLogLatitudes, List<Double> prevLogLongitudes,
            double routeDeviationMeters, double haltProximityMeters) {

        ComplianceResult.ComplianceResultBuilder resultBuilder =
                ComplianceResult.builder();

        int haltCount = haltLatitudes.size();
        if (haltCount == 0) {
            // No halts defined — nothing to check.
            return resultBuilder
                    .routeDeviation(false)
                    .minDistanceToRouteMeters(0)
                    .build();
        }

        // ---- 1. Route deviation check -------------------------------- //
        double minSegmentDist = checkRouteDeviation(
                currentLat, currentLng,
                haltLatitudes, haltLongitudes);

        boolean deviated = minSegmentDist > routeDeviationMeters;
        resultBuilder
                .routeDeviation(deviated)
                .minDistanceToRouteMeters(
                        Math.round(minSegmentDist * 100.0) / 100.0);

        // ---- 2. Nearest halt ----------------------------------------- //
        int nearestIdx = findNearestHalt(
                currentLat, currentLng,
                haltLatitudes, haltLongitudes);

        resultBuilder
                .nearestHaltOrder(haltOrders.get(nearestIdx))
                .nearestHaltName(haltNames.get(nearestIdx));

        // ---- 3. Halt skip check -------------------------------------- //
        List<String> skipped = checkHaltSkipping(
                currentLat, currentLng, nearestIdx,
                haltLatitudes, haltLongitudes, haltNames,
                prevLogLatitudes, prevLogLongitudes,
                haltProximityMeters);

        resultBuilder.skippedHaltNames(skipped);

        return resultBuilder.build();
    }

    /**
     * Convenience overload using default thresholds.
     */
    public static ComplianceResult analyseCompliance(
            double currentLat, double currentLng,
            List<Double> haltLatitudes, List<Double> haltLongitudes,
            List<String> haltNames, List<Integer> haltOrders,
            List<Double> prevLogLatitudes, List<Double> prevLogLongitudes) {

        return analyseCompliance(
                currentLat, currentLng,
                haltLatitudes, haltLongitudes, haltNames, haltOrders,
                prevLogLatitudes, prevLogLongitudes,
                DEFAULT_ROUTE_DEVIATION_METERS,
                DEFAULT_HALT_PROXIMITY_METERS);
    }

    // ------------------------------------------------------------------ //
    //  Internal algorithms                                                //
    // ------------------------------------------------------------------ //

    /**
     * Compute the minimum perpendicular distance from a GPS point
     * to any route segment (consecutive halt pair).
     */
    static double checkRouteDeviation(
            double lat, double lng,
            List<Double> haltLats, List<Double> haltLngs) {

        if (haltLats.size() == 1) {
            return haversineMeters(lat, lng,
                    haltLats.get(0), haltLngs.get(0));
        }

        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < haltLats.size() - 1; i++) {
            double d = pointToSegmentDistance(
                    lat, lng,
                    haltLats.get(i), haltLngs.get(i),
                    haltLats.get(i + 1), haltLngs.get(i + 1));
            if (d < minDist) {
                minDist = d;
            }
        }
        return minDist;
    }

    /**
     * Find the index of the halt nearest to the given GPS point.
     */
    static int findNearestHalt(
            double lat, double lng,
            List<Double> haltLats, List<Double> haltLngs) {

        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < haltLats.size(); i++) {
            double d = haversineMeters(lat, lng,
                    haltLats.get(i), haltLngs.get(i));
            if (d < minDist) {
                minDist = d;
                nearest = i;
            }
        }
        return nearest;
    }

    /**
     * Detect skipped halts.
     * <p>
     * Builds a set of "visited" halts from previous GPS logs (any log
     * within {@code proximityMeters} of a halt counts as a visit).
     * If the bus is currently nearest to halt N, any halt between
     * the last visited halt and N that was never visited is "skipped".
     */
    static List<String> checkHaltSkipping(
            double currentLat, double currentLng, int nearestIdx,
            List<Double> haltLats, List<Double> haltLngs,
            List<String> haltNames,
            List<Double> logLats, List<Double> logLngs,
            double proximityMeters) {

        int haltCount = haltLats.size();
        Set<Integer> visited = new HashSet<>();

        // Mark halts visited by previous GPS logs.
        for (int logIdx = 0; logIdx < logLats.size(); logIdx++) {
            double logLat = logLats.get(logIdx);
            double logLng = logLngs.get(logIdx);
            for (int h = 0; h < haltCount; h++) {
                double dist = haversineMeters(logLat, logLng,
                        haltLats.get(h), haltLngs.get(h));
                if (dist <= proximityMeters) {
                    visited.add(h);
                }
            }
        }

        // Also check current position.
        for (int h = 0; h < haltCount; h++) {
            double dist = haversineMeters(currentLat, currentLng,
                    haltLats.get(h), haltLngs.get(h));
            if (dist <= proximityMeters) {
                visited.add(h);
            }
        }

        // Determine the highest-order halt that has been visited
        // (up to and including the current nearest halt).
        int highestVisited = -1;
        for (int h = 0; h <= nearestIdx; h++) {
            if (visited.contains(h)) {
                highestVisited = h;
            }
        }

        // Find the earliest visited halt to define the starting boundary.
        int earliestVisited = -1;
        for (int h = 0; h < haltCount; h++) {
            if (visited.contains(h)) {
                earliestVisited = h;
                break;
            }
        }

        // Any halt between earliestVisited and highestVisited (or
        // nearestIdx) that was NOT visited is considered skipped.
        List<String> skipped = new ArrayList<>();
        if (earliestVisited >= 0) {
            int upperBound = Math.max(highestVisited, nearestIdx);
            for (int h = earliestVisited + 1; h <= upperBound; h++) {
                if (!visited.contains(h)) {
                    skipped.add(haltNames.get(h));
                }
            }
        }

        return skipped;
    }

    // ------------------------------------------------------------------ //
    //  Geo math helpers                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Haversine distance between two GPS points, in meters.
     */
    public static double haversineMeters(
            double lat1, double lng1, double lat2, double lng2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /**
     * Minimum distance (meters) from point P to the line segment AB.
     * <p>
     * Projects P onto AB; if the projection falls outside the segment,
     * the distance to the nearest endpoint is returned instead.
     */
    static double pointToSegmentDistance(
            double pLat, double pLng,
            double aLat, double aLng,
            double bLat, double bLng) {

        double apLat = pLat - aLat;
        double apLng = pLng - aLng;
        double abLat = bLat - aLat;
        double abLng = bLng - aLng;

        double abLenSq = abLat * abLat + abLng * abLng;
        if (abLenSq == 0) {
            // A and B are the same point.
            return haversineMeters(pLat, pLng, aLat, aLng);
        }

        // Parameter t of the projection of P onto line AB.
        double t = (apLat * abLat + apLng * abLng) / abLenSq;
        t = Math.max(0, Math.min(1, t)); // clamp to segment

        double projLat = aLat + t * abLat;
        double projLng = aLng + t * abLng;

        return haversineMeters(pLat, pLng, projLat, projLng);
    }
}
