package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.LocationTrackingRequestDTO;
import com.bustrackpro.dto.response.tracking.RouteStatusResponseDTO;
import com.bustrackpro.exception.RouteNotFoundException;
import com.bustrackpro.modal.route.BusRouteHalt;
import com.bustrackpro.repository.BusRouteRepository;
import com.bustrackpro.repository.BusRouteHaltRepository;
import com.bustrackpro.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Implementation of RouteTrackingService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteTrackingServiceImpl implements RouteTrackingService {

    private final BusRouteRepository routeRepository;
    private final BusRouteHaltRepository routeHaltRepository;

    private static final double TOLERANCE_RADIUS = 200.0; // in meters
    private static final String STATUS_ON_ROUTE = "ON_ROUTE";
    private static final String STATUS_VIOLATION = "ROUTE_VIOLATION";

    @Override
    public RouteStatusResponseDTO detectRouteViolation(LocationTrackingRequestDTO request) {
        log.info("Detecting route violation for routeId: {}", request.routeId());

        // 1. Fetch the route and ensure it exists
        var route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new RouteNotFoundException("Route not found with ID: " + request.routeId()));

        // 2. Fetch associated halts for the route
        List<BusRouteHalt> routeHalts = routeHaltRepository.findByBusRouteOrderByStopOrderAsc(route);

        if (routeHalts.isEmpty()) {
            log.warn("No halts defined for route ID: {}", request.routeId());
            return new RouteStatusResponseDTO(STATUS_VIOLATION, null, "No halts defined");
        }

        // 3. Calculate distance to each halt and find the nearest one
        record HaltDistance(String name, double distance) {}

        HaltDistance nearestHalt = routeHalts.stream()
                .map(rh -> new HaltDistance(
                        rh.getBusHalt().getName(),
                        GeoUtils.haversine(
                                request.currentLatitude(),
                                request.currentLongitude(),
                                rh.getBusHalt().getLatitude(),
                                rh.getBusHalt().getLongitude()
                        )
                ))
                .min(Comparator.comparingDouble(HaltDistance::distance))
                .orElseThrow();

        log.info("Nearest halt: {} at {} meters", nearestHalt.name(), nearestHalt.distance());

        // 4. Determine status based on TOLERANCE_RADIUS
        String status = (nearestHalt.distance() <= TOLERANCE_RADIUS) ? STATUS_ON_ROUTE : STATUS_VIOLATION;

        return new RouteStatusResponseDTO(
                status,
                Math.round(nearestHalt.distance() * 100.0) / 100.0,
                nearestHalt.name()
        );
    }
}
