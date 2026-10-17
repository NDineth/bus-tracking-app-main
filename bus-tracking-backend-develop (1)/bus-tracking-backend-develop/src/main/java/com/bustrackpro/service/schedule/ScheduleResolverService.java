package com.bustrackpro.service.schedule;

import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.Timetable;
import com.bustrackpro.repository.schedule.TimetableRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to resolve the effective active schedule based on priority.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleResolverService {

    private final TimetableRepository timetableRepository;

    /**
     * Determines the highest priority active schedule for a given date and route.
     * Priority: HOLIDAY (3) > WEEKEND (2) > WEEKDAY (1).
     *
     * @param date    - the date
     * @param routeId - the route id
     * @return the highest priority Timetable, or null if none exists
     */
    public Timetable determineActiveSchedule(LocalDate date, long routeId) {
        DayType dayType = ScheduleDateHelper.determineDayType(date);
        
        // Fetch all active candidate timetables matching the day type
        List<Timetable> candidates = timetableRepository.findActiveTimetablesByDayType(routeId, date, dayType);
        
        if (candidates.isEmpty()) {
            return null;
        }

        // Sort descending by priority
        candidates.sort((t1, t2) -> Integer.compare(
                ScheduleDateHelper.getSchedulePriority(t2.getScheduleType()),
                ScheduleDateHelper.getSchedulePriority(t1.getScheduleType())
        ));

        Timetable effectiveTimetable = candidates.get(0);
        log.debug("Effective schedule for route {} on {} is {} (Priority: {})", 
                routeId, date, effectiveTimetable.getScheduleType(), 
                ScheduleDateHelper.getSchedulePriority(effectiveTimetable.getScheduleType()));
        
        return effectiveTimetable;
    }
}
