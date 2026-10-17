package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.LocationTrackingRequestDTO;
import com.bustrackpro.dto.response.tracking.RouteStatusResponseDTO;

/**
 * Service interface for route tracking.
 */
public interface RouteTrackingService {

    /**
     * Detects route violation based on current location.
     *
     * @param request The location tracking request
     * @return The route status response
     */
    RouteStatusResponseDTO detectRouteViolation(LocationTrackingRequestDTO request);
}
