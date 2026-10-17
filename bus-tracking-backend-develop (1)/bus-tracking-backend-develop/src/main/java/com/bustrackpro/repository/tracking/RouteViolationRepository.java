package com.bustrackpro.repository.tracking;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.tracking.RouteViolation;
import com.bustrackpro.modal.tracking.ViolationType;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Route violation repository.
 */
@Repository
public interface RouteViolationRepository
        extends JpaRepository<RouteViolation, Integer> {

    /**
     * Find all violations for a schedule assignment.
     */
    List<RouteViolation> findByScheduleAssignment(
            ScheduleAssignment scheduleAssignment);

    /**
     * Find violations detected after a given time (active/recent violations).
     */
    @Query("SELECT v FROM RouteViolation v WHERE v.detectedAt >= :since ORDER BY v.detectedAt DESC")
    List<RouteViolation> findRecentViolations(@Param("since") LocalDateTime since);

    /**
     * Find violations for a specific bus, ordered by time descending.
     */
    List<RouteViolation> findByBusOrderByDetectedAtDesc(Bus bus);

    /**
     * Find violations within a date range.
     */
    @Query("SELECT v FROM RouteViolation v "
         + "WHERE v.detectedAt BETWEEN :from AND :to "
         + "ORDER BY v.detectedAt DESC")
    List<RouteViolation> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Find violations by bus and date range.
     */
    @Query("SELECT v FROM RouteViolation v "
         + "WHERE v.bus.id = :busId "
         + "AND v.detectedAt BETWEEN :from AND :to "
         + "ORDER BY v.detectedAt DESC")
    List<RouteViolation> findByBusIdAndDateRange(
            @Param("busId") Integer busId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Find violations by type within a date range.
     */
    @Query("SELECT v FROM RouteViolation v "
         + "WHERE v.violationType = :type "
         + "AND v.detectedAt BETWEEN :from AND :to "
         + "ORDER BY v.detectedAt DESC")
    List<RouteViolation> findByTypeAndDateRange(
            @Param("type") ViolationType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Count violations for a schedule assignment.
     */
    long countByScheduleAssignment(ScheduleAssignment scheduleAssignment);

    /**
     * Count violations detected after a given time.
     */
    long countByDetectedAtAfter(LocalDateTime since);

    /**
     * Find violations by bus id, ordered by detection time descending.
     */
    List<RouteViolation> findByBusIdOrderByDetectedAtDesc(Integer busId);
}

