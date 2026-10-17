package com.bustrackpro.controller.tracking;

import com.bustrackpro.dto.request.tracking.GpsUpdateRequest;
import com.bustrackpro.dto.response.tracking.*;
import com.bustrackpro.service.tracking.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for real-time GPS tracking and route compliance.
 */
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * Receive a GPS update from a bus and return compliance analysis.
     *
     * @param request - GPS update data (busId, latitude, longitude, etc.)
     * @return tracking response with route compliance status
     */
    @PostMapping("/gps")
    public ResponseEntity<TrackingResponse> processGpsUpdate(
            @Valid @RequestBody GpsUpdateRequest request) {
        return ResponseEntity.ok(
                trackingService.processGpsUpdate(request));
    }

    /**
     * Get real-time active bus locations.
     *
     * @return list of active bus locations
     */
    @org.springframework.web.bind.annotation.GetMapping("/locations/active")
    public ResponseEntity<List<ActiveBusLocationResponse>> getActiveBusLocations() {
        return ResponseEntity.ok(trackingService.getActiveBusLocations());
    }

    /**
     * Get the latest GPS location for a specific bus.
     */
    @GetMapping("/bus/{busId}/location")
    public ResponseEntity<ActiveBusLocationResponse> getBusLocation(@PathVariable Integer busId) {
        return ResponseEntity.ok(trackingService.getBusLocation(busId));
    }

    /**
     * Get GPS log history for a bus, optionally filtered by date range or limited to recent logs.
     */
    @GetMapping("/bus/{busId}/gps-logs")
    public ResponseEntity<List<GpsLogResponse>> getGpsLogs(
            @PathVariable Integer busId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(trackingService.getGpsLogs(busId, from, to, limit));
    }

    /**
     * Get GPS log history for a specific schedule assignment (bus running schedule).
     */
    @GetMapping("/schedule-assignment/{scheduleAssignmentId}/gps-logs")
    public ResponseEntity<List<GpsLogResponse>> getGpsLogsByScheduleAssignment(
            @PathVariable Integer scheduleAssignmentId) {
        return ResponseEntity.ok(trackingService.getGpsLogsByScheduleAssignment(scheduleAssignmentId));
    }

    /**
     * Get the active trip details for a bus.
     */
    @GetMapping("/bus/{busId}/trip/active")
    public ResponseEntity<TripHistoryResponse> getActiveTrip(@PathVariable Integer busId) {
        return ResponseEntity.ok(trackingService.getActiveTrip(busId));
    }

    /**
     * Get trip history, optionally filtered by bus, route, and date range.
     */
    @GetMapping("/trips/history")
    public ResponseEntity<List<TripHistoryResponse>> getTripHistory(
            @RequestParam(required = false) Integer busId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(trackingService.getTripHistory(busId, routeId, from, to));
    }

    /**
     * Get violation records, optionally filtered.
     */
    @GetMapping("/violations")
    public ResponseEntity<List<ViolationResponse>> getViolations(
            @RequestParam(required = false) Integer busId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(trackingService.getViolations(busId, type, from, to));
    }

    /**
     * Get currently active violations (detected in the last hour).
     */
    @GetMapping("/violations/active")
    public ResponseEntity<List<ViolationResponse>> getActiveViolations() {
        return ResponseEntity.ok(trackingService.getActiveViolations());
    }

    /**
     * Get halt visit status for a bus on its current trip.
     */
    @GetMapping("/bus/{busId}/halt-status")
    public ResponseEntity<List<HaltStatusResponse>> getHaltStatus(@PathVariable Integer busId) {
        return ResponseEntity.ok(trackingService.getHaltStatus(busId));
    }

    /**
     * Get dashboard summary statistics.
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(trackingService.getDashboardSummary());
    }
}
