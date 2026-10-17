package com.bustrackpro.repository.tracking;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.tracking.DrivingBehaviorType;
import com.bustrackpro.modal.tracking.DrivingStatusLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for driving status log entries.
 */
@Repository
public interface DrivingStatusLogRepository extends JpaRepository<DrivingStatusLog, Integer> {

    /**
     * Find all status logs for a given bus, ordered by recorded time descending.
     */
    List<DrivingStatusLog> findByBusOrderByRecordedAtDesc(Bus bus);

    /**
     * Find the most recent status log for a bus.
     */
    Optional<DrivingStatusLog> findTopByBusOrderByRecordedAtDesc(Bus bus);

    /**
     * Find status logs for a bus within a time range.
     */
    List<DrivingStatusLog> findByBusAndRecordedAtBetweenOrderByRecordedAtAsc(
            Bus bus, LocalDateTime from, LocalDateTime to);

    /**
     * Find status logs by bus and status type.
     */
    List<DrivingStatusLog> findByBusAndStatusOrderByRecordedAtDesc(
            Bus bus, DrivingBehaviorType status);
}
