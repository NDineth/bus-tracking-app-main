package com.bustrackpro.dto.response.tracking;

/**
 * DTO for route status response.
 */
public record RouteStatusResponseDTO(
        String status,
        Double distanceToNearestHalt,
        String nearestHaltName
) {
}
