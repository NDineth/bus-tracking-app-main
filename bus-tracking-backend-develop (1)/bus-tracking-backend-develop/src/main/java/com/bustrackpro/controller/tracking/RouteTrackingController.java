package com.bustrackpro.controller.tracking;

import com.bustrackpro.dto.request.tracking.LocationTrackingRequestDTO;
import com.bustrackpro.dto.response.tracking.RouteStatusResponseDTO;
import com.bustrackpro.service.tracking.RouteTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for route tracking and violation detection.
 */
@RestController
@RequestMapping("tracking")
@RequiredArgsConstructor
public class RouteTrackingController {

    private final RouteTrackingService routeTrackingService;

    /**
     * Endpoint to detect route violations in real-time.
     *
     * @param request The location tracking request
     * @return The route status response
     */
    @PostMapping("/route-violation")
    public ResponseEntity<RouteStatusResponseDTO> detectRouteViolation(
            @Valid @RequestBody LocationTrackingRequestDTO request) {
        return ResponseEntity.ok(routeTrackingService.detectRouteViolation(request));
    }
}
