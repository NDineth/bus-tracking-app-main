package com.bustrackpro.repository.tracking;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.tracking.GpsLog;

import java.security.Timestamp;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * GPS log repository.
 */
@Repository
public interface GpsLogRepository extends JpaRepository<GpsLog, Integer> {

    /**
     * Find the most recent GPS log entry for a bus.
     *
     * @param bus - the bus
     * @return optional GPS log
     */
    Optional<GpsLog> findTopByBusOrderByRecordedAtDesc(Bus bus);

    /**
     * Find all GPS logs for a schedule assignment, ordered by time.
     *
     * @param scheduleAssignment - the assignment
     * @return ordered list of GPS logs
     */
    List<GpsLog> findByScheduleAssignmentOrderByRecordedAtAsc(
            ScheduleAssignment scheduleAssignment);

    /**
     * Find the latest GPS log for each active bus within the given time frame.
     *
     * @param since - the time frame start
     * @return list of latest active GPS logs
     */
    @Query(value = """
    SELECT 
        sub.id,
        sub.bus_id,
        sub.created_at,
        sub.latitude,
        sub.longitude,
        sub.recorded_at,
        sub.schedule_assignment_id,
        sub.speed
    FROM (
        SELECT 
            gl.*, 
            ROW_NUMBER() OVER (PARTITION BY gl.bus_id ORDER BY gl.recorded_at DESC) as rn
        FROM gps_log gl
        INNER JOIN schedule_assignment sa ON gl.schedule_assignment_id = sa.id
        WHERE sa.status = 'IN_PROGRESS'
          AND gl.recorded_at >= :recordedAt
    ) sub
    WHERE sub.rn = 1
    """, nativeQuery = true)
    List<GpsLog> findLatestGpsLogs(@Param("recordedAt") LocalDateTime recordedAt);

    /**
     * Find GPS logs for a bus within a date range, ordered by time descending.
     */
    @Query("SELECT g FROM GpsLog g WHERE g.bus.id = :busId "
         + "AND g.recordedAt BETWEEN :from AND :to "
         + "ORDER BY g.recordedAt DESC")
    List<GpsLog> findByBusIdAndDateRange(
            @Param("busId") Integer busId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Find the most recent N GPS logs for a bus (for GPS trail).
     */
    @Query(value = "SELECT * FROM gps_log WHERE bus_id = :busId "
         + "ORDER BY recorded_at DESC LIMIT :limit",
         nativeQuery = true)
    List<GpsLog> findRecentByBusId(
            @Param("busId") Integer busId,
            @Param("limit") int limit);

    /**
     * Find the latest GPS log for a specific bus.
     */
    Optional<GpsLog> findTopByBusIdOrderByRecordedAtDesc(Integer busId);

    /**
     * Count GPS logs for a schedule assignment.
     */
    long countByScheduleAssignment(ScheduleAssignment scheduleAssignment);

    /**
     * Count distinct buses that have GPS logs within a date range.
     */
    @Query("SELECT COUNT(DISTINCT g.bus.id) FROM GpsLog g WHERE g.recordedAt BETWEEN :from AND :to")
    int countDistinctBusesByRecordedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
