package com.bustrackpro.service.task;

import com.bustrackpro.service.schedule.AutoScheduleService;
import com.bustrackpro.service.schedule.ScheduleAssignmentService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron job for automatically generating daily bus schedules and monitoring start statuses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleCronJob {

    private final AutoScheduleService autoScheduleService;
    private final ScheduleAssignmentService scheduleAssignmentService;

    /**
     * Automatically generates the schedule for the next day.
     * Runs at 2:00 AM every day, Sri Lanka Standard Time.
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Colombo")
    public void generateSchedulesAutomatically() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        log.info("Cron Job triggered: Generating auto-schedule for {}", targetDate);
        
        try {
            autoScheduleService.generateDailySchedule(targetDate);
            log.info("Successfully generated auto-schedule for {}", targetDate);
        } catch (Exception e) {
            log.error("Failed to generate auto-schedule for {}: {}", targetDate, e.getMessage(), e);
        }
    }

    /**
     * Periodically check for scheduled assignments that did not start within 30 minutes.
     * Runs every minute.
     */
    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Colombo")
    public void autoDetectUnstartedBuses() {
        log.debug("Cron Job triggered: Checking for unstarted bus schedules");
        try {
            scheduleAssignmentService.autoDetectUnstartedBuses();
        } catch (Exception e) {
            log.error("Failed to auto-detect unstarted buses: {}", e.getMessage(), e);
        }
    }
}
