package com.bustrackpro.controller.schedule;

import com.bustrackpro.dto.request.schedule.TimetableRequest;
import com.bustrackpro.dto.response.schedule.TimetableResponse;
import com.bustrackpro.service.schedule.TimetableService;
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
 * Controller for timetable operations.
 */
@RestController
@RequestMapping("/schedules/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    /**
     * Create a new timetable.
     *
     * @param request - the timetable request DTO
     * @return the created timetable response
     */
    @PostMapping
    public ResponseEntity<TimetableResponse> createTimetable(
            @Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.ok(
                timetableService.createTimetable(request));
    }

    /**
     * Get all timetables.
     *
     * @return list of timetable responses
     */
    @GetMapping
    public ResponseEntity<List<TimetableResponse>> getAllTimetables() {
        return ResponseEntity.ok(timetableService.getAllTimetables());
    }

    /**
     * Get timetable by id.
     *
     * @param id - the timetable id
     * @return the timetable response
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimetableResponse> getTimetableById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(timetableService.getTimetableById(id));
    }

    /**
     * Get timetables for a specific route.
     *
     * @param routeId - the route id
     * @return list of timetable responses
     */
    @GetMapping("/route/{routeId}")
    public ResponseEntity<List<TimetableResponse>> getTimetablesByRoute(
            @PathVariable Integer routeId) {
        return ResponseEntity.ok(
                timetableService.getTimetablesByRoute(routeId));
    }

    /**
     * Update a timetable.
     *
     * @param id      - the timetable id
     * @param request - the updated timetable request DTO
     * @return the updated timetable response
     */
    @PutMapping("/{id}")
    public ResponseEntity<TimetableResponse> updateTimetable(
            @PathVariable Integer id,
            @Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.ok(
                timetableService.updateTimetable(id, request));
    }

    /**
     * Delete a timetable.
     *
     * @param id - the timetable id
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimetable(
            @PathVariable Integer id) {
        timetableService.deleteTimetable(id);
        return ResponseEntity.noContent().build();
    }
}
