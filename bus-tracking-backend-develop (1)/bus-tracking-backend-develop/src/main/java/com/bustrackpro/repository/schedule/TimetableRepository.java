package com.bustrackpro.repository.schedule;

import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.schedule.DayType;
import com.bustrackpro.modal.schedule.ScheduleType;
import com.bustrackpro.modal.schedule.Timetable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Timetable repository.
 */
@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Integer> {

    /**
     * Find all timetables for a route.
     *
     * @param busRoute - the bus route
     * @return list of timetables
     */
    List<Timetable> findByBusRoute(BusRoute busRoute);

    /**
     * Find timetables for a route filtered by day type.
     *
     * @param busRoute - the bus route
     * @param dayType  - the day type
     * @return list of timetables
     */
    List<Timetable> findByBusRouteAndDayType(BusRoute busRoute, DayType dayType);

    /**
     * Find active timetables for a route on a given date.
     *
     * @param routeId      - the route id
     * @param date         - the target date
     * @param dayType      - the day type for the date
     * @param scheduleType - the schedule type for the date
     * @return list of active timetables
     */
    @Query("SELECT t FROM Timetable t WHERE t.busRoute.id = :routeId "
           + "AND t.dayType = :dayType "
           + "AND t.scheduleType = :scheduleType "
           + "AND t.activeDate <= :date "
           + "AND t.expirationDate >= :date")
    List<Timetable> findActiveTimetables(
            @Param("routeId") long routeId,
            @Param("date") LocalDate date,
            @Param("dayType") DayType dayType,
            @Param("scheduleType") ScheduleType scheduleType);

    /**
     * Find active timetables for a route on a given date by day type only.
     *
     * @param routeId - the route id
     * @param date    - the target date
     * @param dayType - the day type for the date
     * @return list of candidate timetables
     */
    @Query("SELECT t FROM Timetable t WHERE t.busRoute.id = :routeId "
           + "AND t.dayType = :dayType "
           + "AND t.activeDate <= :date "
           + "AND t.expirationDate >= :date")
    List<Timetable> findActiveTimetablesByDayType(
            @Param("routeId") long routeId,
            @Param("date") LocalDate date,
            @Param("dayType") DayType dayType);

    /**
     * Count overlapping timetables for the same route, day type, and schedule type.
     *
     * @param routeId        - the route id
     * @param dayType        - the day type
     * @param scheduleType   - the schedule type
     * @param activeDate     - proposed active date
     * @param expirationDate - proposed expiration date
     * @param excludeId      - timetable id to exclude (for updates)
     * @return count of overlapping timetables
     */
    @Query("SELECT COUNT(t) FROM Timetable t WHERE t.busRoute.id = :routeId "
           + "AND t.dayType = :dayType "
           + "AND t.scheduleType = :scheduleType "
           + "AND t.activeDate <= :expirationDate "
           + "AND t.expirationDate >= :activeDate "
           + "AND (:excludeId IS NULL OR t.id != :excludeId)")
    long countOverlapping(
            @Param("routeId") long routeId,
            @Param("dayType") DayType dayType,
            @Param("scheduleType") ScheduleType scheduleType,
            @Param("activeDate") LocalDate activeDate,
            @Param("expirationDate") LocalDate expirationDate,
            @Param("excludeId") Integer excludeId);

    /**
     * Find timetables active in a route within a period.
     *
     * @param routeId   - the route id
     * @param startDate - period start
     * @param endDate   - period end
     * @return list of active timetables
     */
    @Query("SELECT t FROM Timetable t WHERE t.busRoute.id = :routeId "
           + "AND t.activeDate <= :endDate "
           + "AND t.expirationDate >= :startDate")
    List<Timetable> findTimetablesActiveInPeriod(
            @Param("routeId") long routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
