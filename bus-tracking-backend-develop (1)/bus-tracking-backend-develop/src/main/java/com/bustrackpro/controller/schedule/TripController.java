package com.bustrackpro.controller.schedule;

import com.bustrackpro.dto.request.schedule.TripRequest;
import com.bustrackpro.dto.response.schedule.TripResponse;
import com.bustrackpro.service.schedule.TripService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for trip operations within timetables.
 */
@RestController
@RequestMapping("/schedules/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /**
     * Add a trip to a timetable.
     *
     * @param request - the trip request DTO
     * @return the created trip response
     */
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(tripService.createTrip(request));
    }

    /**
     * Get all trips for a timetable.
     *
     * @param timetableId - the timetable id
     * @return list of trip responses
     */
    @GetMapping("/timetable/{timetableId}")
    public ResponseEntity<List<TripResponse>> getTripsByTimetable(
            @PathVariable Integer timetableId) {
        return ResponseEntity.ok(
                tripService.getTripsByTimetable(timetableId));
    }

    /**
     * Get a trip by id.
     *
     * @param id - the trip id
     * @return the trip response
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    /**
     * Update a trip.
     *
     * @param id      - the trip id
     * @param request - the updated trip request DTO
     * @return the updated trip response
     */
    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Integer id,
            @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(id, request));
    }

    /**
     * Delete a trip.
     *
     * @param id - the trip id
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Integer id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
