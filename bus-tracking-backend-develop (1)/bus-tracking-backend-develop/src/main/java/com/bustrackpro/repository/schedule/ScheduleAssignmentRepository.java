package com.bustrackpro.repository.schedule;

import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.schedule.Trip;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Schedule assignment repository.
 */
@Repository
public interface ScheduleAssignmentRepository
        extends JpaRepository<ScheduleAssignment, Integer> {

    /**
     * Find assignment for a trip on a specific date.
     *
     * @param trip           - the trip
     * @param assignmentDate - the assignment date
     * @return optional assignment
     */
    Optional<ScheduleAssignment> findByTripAndAssignmentDate(
            Trip trip, LocalDate assignmentDate);

    /**
     * Find all assignments for a specific date.
     *
     * @param assignmentDate - the date
     * @return list of assignments
     */
    List<ScheduleAssignment> findByAssignmentDate(LocalDate assignmentDate);

    /**
     * Count bus conflicts for overlapping trips on a given date.
     *
     * @param busId          - the bus id
     * @param assignmentDate - the target date
     * @param departureTime  - new trip departure time
     * @param arrivalTime    - new trip arrival time
     * @return count of conflicting assignments
     */
    @Query("SELECT COUNT(sa) FROM ScheduleAssignment sa "
           + "WHERE sa.bus.id = :busId "
           + "AND sa.assignmentDate = :assignmentDate "
           + "AND sa.trip.departureTime < :arrivalTime "
           + "AND sa.trip.arrivalTime > :departureTime "
           + "AND sa.status NOT IN (com.bustrackpro.modal.schedule.AssignmentStatus.CANCELLED, com.bustrackpro.modal.schedule.AssignmentStatus.TURN_LOSS)")
    long countBusConflicts(
            @Param("busId") Integer busId,
            @Param("assignmentDate") LocalDate assignmentDate,
            @Param("departureTime") LocalTime departureTime,
            @Param("arrivalTime") LocalTime arrivalTime);

    /**
     * Count driver conflicts for overlapping trips on a given date.
     *
     * @param driverId       - the driver id
     * @param assignmentDate - the target date
     * @param departureTime  - new trip departure time
     * @param arrivalTime    - new trip arrival time
     * @return count of conflicting assignments
     */
    @Query("SELECT COUNT(sa) FROM ScheduleAssignment sa "
           + "WHERE sa.driver.id = :driverId "
           + "AND sa.assignmentDate = :assignmentDate "
           + "AND sa.trip.departureTime < :arrivalTime "
           + "AND sa.trip.arrivalTime > :departureTime "
           + "AND sa.status NOT IN (com.bustrackpro.modal.schedule.AssignmentStatus.CANCELLED, com.bustrackpro.modal.schedule.AssignmentStatus.TURN_LOSS)")
    long countDriverConflicts(
            @Param("driverId") Integer driverId,
            @Param("assignmentDate") LocalDate assignmentDate,
            @Param("departureTime") LocalTime departureTime,
            @Param("arrivalTime") LocalTime arrivalTime);

    /**
     * Find assignments for a bus within a date range.
     *
     * @param busId     - the bus id
     * @param startDate - range start date
     * @param endDate   - range end date
     * @return list of assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.bus.id = :busId "
           + "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    List<ScheduleAssignment> findByBusAndDateRange(
            @Param("busId") Integer busId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find assignments for a route within a date range.
     *
     * @param routeId   - the route id
     * @param startDate - range start date
     * @param endDate   - range end date
     * @return list of assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.trip.timetable.busRoute.id = :routeId "
           + "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    List<ScheduleAssignment> findByRouteAndDateRange(
            @Param("routeId") Long routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find assignments for a list of trips on a specific date.
     *
     * @param trips          - the trips
     * @param assignmentDate - the date
     * @return list of assignments
     */
    List<ScheduleAssignment> findByTripInAndAssignmentDate(
            List<Trip> trips, LocalDate assignmentDate);

    /**
     * Find assignments for a list of trips within a date range.
     *
     * @param trips     - the trips
     * @param startDate - range start date
     * @param endDate   - range end date
     * @return list of assignments
     */
    List<ScheduleAssignment> findByTripInAndAssignmentDateBetween(
            List<Trip> trips, LocalDate startDate, LocalDate endDate);

    /**
     * Delete all assignments belonging to a trip.
     *
     * @param trip - the trip
     */
    void deleteByTrip(Trip trip);

    /**
     * Find assignments for a bus on a specific date with given statuses,
     * ordered by trip departure time.
     *
     * @param busId    - the bus id
     * @param date     - the assignment date
     * @param statuses - allowed statuses
     * @return list of matching assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.bus.id = :busId "
           + "AND sa.assignmentDate = :date "
           + "AND sa.status IN :statuses "
           + "ORDER BY sa.trip.departureTime ASC")
    List<ScheduleAssignment> findByBusIdAndDateAndStatusIn(
            @Param("busId") Integer busId,
            @Param("date") LocalDate date,
            @Param("statuses") List<AssignmentStatus> statuses);

    /**
     * Find all assignments across the fleet within a date range with given statuses.
     * Used for the Admin finance summary endpoint.
     *
     * @param startDate - range start
     * @param endDate   - range end
     * @param statuses  - allowed statuses
     * @return list of assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.assignmentDate BETWEEN :startDate AND :endDate "
           + "AND sa.status IN :statuses")
    List<ScheduleAssignment> findAllByDateRangeAndStatusIn(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<AssignmentStatus> statuses);

    /**
     * Find assignments for a specific bus within a date range with given statuses.
     *
     * @param busId     - the bus id
     * @param startDate - range start
     * @param endDate   - range end
     * @param statuses  - allowed statuses
     * @return list of assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.bus.id = :busId "
           + "AND sa.assignmentDate BETWEEN :startDate AND :endDate "
           + "AND sa.status IN :statuses")
    List<ScheduleAssignment> findByBusIdAndDateRangeAndStatusIn(
            @Param("busId") Integer busId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<AssignmentStatus> statuses);

    /**
     * Find assignments for a route within a date range.
     *
     * @param routeId   - the route id
     * @param startDate - range start date
     * @param endDate   - range end date
     * @return list of assignments
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.trip.timetable.busRoute.id = :routeId "
           + "AND sa.assignmentDate BETWEEN :startDate AND :endDate")
    List<ScheduleAssignment> findByRouteIdAndAssignmentDateBetween(
            @Param("routeId") long routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find next scheduled assignments for a bus chronologically.
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE sa.bus.id = :busId "
           + "AND ((sa.assignmentDate = :date AND sa.trip.departureTime > :time) OR sa.assignmentDate > :date) "
           + "AND sa.status = com.bustrackpro.modal.schedule.AssignmentStatus.SCHEDULED "
           + "ORDER BY sa.assignmentDate ASC, sa.trip.departureTime ASC")
    List<ScheduleAssignment> findNextScheduledAssignments(
            @Param("busId") Integer busId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    /**
     * Find assignments filtered by bus, route, and date.
     */
    @Query("SELECT sa FROM ScheduleAssignment sa "
           + "WHERE (:busId IS NULL OR sa.bus.id = :busId) "
           + "AND (:routeId IS NULL OR sa.trip.timetable.busRoute.id = :routeId) "
           + "AND (:date IS NULL OR sa.assignmentDate = :date) "
           + "ORDER BY sa.assignmentDate DESC, sa.trip.departureTime ASC")
    List<ScheduleAssignment> findAssignmentsFiltered(
            @Param("busId") Integer busId,
            @Param("routeId") Long routeId,
            @Param("date") LocalDate date);

    /**
     * Count the number of assignments for a bus with a given status.
     */
    long countByBusIdAndStatus(Integer busId, AssignmentStatus status);
}
