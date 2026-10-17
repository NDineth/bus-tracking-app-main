package com.bustrackpro.service.tracking;

import com.bustrackpro.dto.request.tracking.GpsUpdateRequest;
import com.bustrackpro.dto.response.tracking.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for GPS tracking, route compliance analysis,
 * and tracking dashboard data.
 */
public interface TrackingService {

    /**
     * Process a GPS update from a bus.
     */
    TrackingResponse processGpsUpdate(GpsUpdateRequest request);

    /**
     * Get a list of the most recent active bus locations.
     */
    List<ActiveBusLocationResponse> getActiveBusLocations();

    /**
     * Get the latest GPS location for a specific bus.
     */
    ActiveBusLocationResponse getBusLocation(Integer busId);

    /**
     * Get GPS log history for a bus, optionally filtered by date range.
     */
    List<GpsLogResponse> getGpsLogs(Integer busId, LocalDate from, LocalDate to, Integer limit);

    /**
     * Get GPS log history for a specific schedule assignment.
     */
    List<GpsLogResponse> getGpsLogsByScheduleAssignment(Integer scheduleAssignmentId);

    /**
     * Get the active trip details for a bus.
     */
    TripHistoryResponse getActiveTrip(Integer busId);

    /**
     * Get trip history, optionally filtered by bus, route, and date range.
     */
    List<TripHistoryResponse> getTripHistory(Integer busId, Long routeId, LocalDate from, LocalDate to);

    /**
     * Get violation records, optionally filtered.
     */
    List<ViolationResponse> getViolations(Integer busId, String type, LocalDate from, LocalDate to);

    /**
     * Get currently active violations (detected in the last hour).
     */
    List<ViolationResponse> getActiveViolations();

    /**
     * Get halt visit status for a bus on its current trip.
     */
    List<HaltStatusResponse> getHaltStatus(Integer busId);

    /**
     * Get dashboard summary statistics.
     */
    DashboardSummaryResponse getDashboardSummary();
}
