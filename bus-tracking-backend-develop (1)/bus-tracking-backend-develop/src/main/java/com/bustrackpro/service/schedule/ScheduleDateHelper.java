package com.bustrackpro.service.schedule;

import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.ScheduleType;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Utility class for schedule date logic.
 */
public final class ScheduleDateHelper {

    private ScheduleDateHelper() {
        // utility class
    }

    /**
     * Determine the day type for a given date.
     *
     * @param date - the date to evaluate
     * @return DayType.WEEKEND for Saturday/Sunday, DayType.WEEKDAY otherwise
     */
    public static DayType determineDayType(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }
        return DayType.WEEKDAY;
    }

    /**
     * Determine the schedule type for a given date.
     *
     * @param date - the date to evaluate
     * @return ScheduleType.WEEKEND for Saturday/Sunday, ScheduleType.WEEKDAY otherwise
     */
    public static ScheduleType determineScheduleType(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return ScheduleType.WEEKEND;
        }
        return ScheduleType.WEEKDAY;
    }

    /**
     * Check if a date falls within a range (inclusive).
     *
     * @param date      - the date to check
     * @param startDate - range start
     * @param endDate   - range end
     * @return true if date is within range
     */
    public static boolean isWithinRange(
            LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Get the priority score for a schedule type.
     * Higher score indicates higher priority.
     * 
     * @param type - the schedule type
     * @return priority score (HOLIDAY = 3, WEEKEND = 2, WEEKDAY = 1)
     */
    public static int getSchedulePriority(ScheduleType type) {
        if (type == null) return 0;
        switch (type) {
            case HOLIDAY:
                return 3;
            case WEEKEND:
                return 2;
            case WEEKDAY:
                return 1;
            default:
                return 0;
        }
    }
}
