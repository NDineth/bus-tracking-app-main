package com.bustrackpro.controller.schedule;

import com.bustrackpro.dto.request.schedule.ScheduleAssignmentRequest;
import com.bustrackpro.dto.response.schedule.AvailableResourcesResponse;
import com.bustrackpro.dto.response.schedule.ScheduleAssignmentResponse;
import com.bustrackpro.dto.response.schedule.ScheduleGridResponse;
import com.bustrackpro.service.schedule.ScheduleAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.bustrackpro.service.schedule.AutoScheduleService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for schedule assignment operations.
 */
@RestController
@RequestMapping("/schedules/assignments")
@RequiredArgsConstructor
public class ScheduleAssignmentController {

    private final ScheduleAssignmentService assignmentService;
    private final AutoScheduleService autoScheduleService;

    /**
     * Create a new schedule assignment with validation.
     *
     * @param request - the assignment request DTO
     * @return the created assignment response
     */
    @PostMapping
    public ResponseEntity<ScheduleAssignmentResponse> createAssignment(
            @Valid @RequestBody ScheduleAssignmentRequest request) {
        return ResponseEntity.ok(
                assignmentService.createAssignment(request));
    }

    /**
     * Get filtered assignments.
     *
     * @param busId   - optional bus filter
     * @param routeId - optional route filter
     * @param date    - optional date filter (yyyy-MM-dd)
     * @return list of schedule assignment responses
     */
    @GetMapping
    public ResponseEntity<List<ScheduleAssignmentResponse>> getAssignments(
            @RequestParam(required = false) Integer busId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(assignmentService.getAssignments(busId, routeId, date));
    }

    /**
     * Get all assignments for a specific date.
     *
     * @param date - the date (yyyy-MM-dd)
     * @return list of assignment responses
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<ScheduleAssignmentResponse>>
            getAssignmentsByDate(@PathVariable String date) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentsByDate(date));
    }

    /**
     * Get an assignment by id.
     *
     * @param id - the assignment id
     * @return the assignment response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleAssignmentResponse> getAssignmentById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentById(id));
    }

    /**
     * Update an assignment.
     *
     * @param id      - the assignment id
     * @param request - the updated assignment request DTO
     * @return the updated assignment response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleAssignmentResponse> updateAssignment(
            @PathVariable Integer id,
            @Valid @RequestBody ScheduleAssignmentRequest request) {
        return ResponseEntity.ok(
                assignmentService.updateAssignment(id, request));
    }

    /**
     * Cancel an assignment (soft delete).
     *
     * @param id - the assignment id
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAssignment(
            @PathVariable Integer id) {
        assignmentService.cancelAssignment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Start an assignment.
     *
     * @param id - the assignment id
     * @return the updated assignment response
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ScheduleAssignmentResponse> startAssignment(
            @PathVariable Integer id) {
        return ResponseEntity.ok(assignmentService.startAssignment(id));
    }

    /**
     * Complete an assignment and trigger fee calculation.
     *
     * @param id - the assignment id
     * @return the completed assignment response with calculated fees
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ScheduleAssignmentResponse> completeAssignment(
            @PathVariable Integer id) {
        return ResponseEntity.ok(assignmentService.completeAssignment(id));
    }


    /**
     * Search for available buses and drivers for a time slot.
     *
     * @param date          - the date (yyyy-MM-dd)
     * @param departureTime - the departure time (HH:mm)
     * @param arrivalTime   - the arrival time (HH:mm)
     * @return available resources response
     */
    @GetMapping("/available")
    public ResponseEntity<AvailableResourcesResponse>
            getAvailableResources(
            @RequestParam String date,
            @RequestParam String departureTime,
            @RequestParam String arrivalTime,
            @RequestParam(required = false) Long routeId) {
        return ResponseEntity.ok(
                assignmentService.getAvailableResources(
                        date, departureTime, arrivalTime, routeId));
    }

    /**
     * Get scheduler grid data for a timetable on a specific date.
     *
     * @param timetableId - the timetable id
     * @param date        - the date (yyyy-MM-dd)
     * @return schedule grid response
     */
    @GetMapping("/grid/{timetableId}")
    public ResponseEntity<ScheduleGridResponse> getScheduleGrid(
            @PathVariable Integer timetableId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(
                assignmentService.getScheduleGrid(timetableId, startDate, endDate));
    }

    /**
     * Manually trigger the auto-scheduling algorithm for a given date.
     *
     * @param date - the date (yyyy-MM-dd) to generate schedule for
     * @return success message
     */
    @PostMapping("/auto-generate")
    public ResponseEntity<String> autoGenerateSchedule(@RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        autoScheduleService.generateDailySchedule(targetDate);
        return ResponseEntity.ok("Auto-schedule generation triggered successfully for " + targetDate);
    }

    /**
     * Manually dispatch timetable and daily schedules to affected bus owners.
     *
     * @param timetableId - timetable ID
     * @param date - target date (yyyy-MM-dd)
     * @return dispatch response count
     */
    @PostMapping("/dispatch-timetable")
    public ResponseEntity<String> dispatchTimetable(
            @RequestParam Integer timetableId,
            @RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        int dispatchedCount = assignmentService.dispatchTimetableToOwners(timetableId, targetDate);
        return ResponseEntity.ok("Timetable dispatched successfully to " + dispatchedCount + " bus owner(s) for " + targetDate);
    }
}
