package com.bustrackpro.modal.schedule;

/**
 * Schedule type enumeration for timetable scheduling.
 * Values must match the database check constraint: WEEKDAY, WEEKEND, HOLIDAY.
 */
public enum ScheduleType {

    WEEKDAY,
    WEEKEND,
    HOLIDAY

}
