package com.bustrackpro.controller.schedule;

import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.service.schedule.ScheduleResolverService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for resolving effective schedules.
 */
@RestController
@RequestMapping("/schedules/effective")
@RequiredArgsConstructor
public class EffectiveScheduleController {

    private final ScheduleResolverService scheduleResolverService;

    @GetMapping
    public ResponseEntity<Map<String, EffectiveScheduleResponse>> getEffectiveSchedules(
            @RequestParam Long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, EffectiveScheduleResponse> response = new HashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Timetable effective = scheduleResolverService.determineActiveSchedule(date, routeId);
            
            EffectiveScheduleResponse status = new EffectiveScheduleResponse();
            if (effective == null) {
                status.setEffectiveScheduleType(null);
                status.setAssignmentAllowed(false);
                status.setBlockReason("No active timetable for this date");
            } else {
                status.setEffectiveScheduleType(effective.getScheduleType().name());
                status.setEffectiveTimetableId(effective.getId());
                status.setAssignmentAllowed(true); // Assignments map to the effective schedule's trips
                
                if (effective.getScheduleType() == com.bustrackpro.modal.schedule.ScheduleType.HOLIDAY) {
                    status.setBlockReason("Holiday schedule takes priority");
                } else if (effective.getScheduleType() == com.bustrackpro.modal.schedule.ScheduleType.WEEKEND) {
                    status.setBlockReason("Weekend schedule takes priority");
                } else {
                    status.setBlockReason("Another schedule takes priority");
                }
            }
            
            response.put(date.toString(), status);
        }

        return ResponseEntity.ok(response);
    }

    @Data
    public static class EffectiveScheduleResponse {
        private String effectiveScheduleType;
        private Integer effectiveTimetableId;
        private boolean assignmentAllowed;
        private String blockReason;
    }
}
