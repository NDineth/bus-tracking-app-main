package com.bustrackpro.modal.finance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a trip delay log record for penalty calculation.
 */
@Entity
@Table(name = "delay_log_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayLogRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bus_number", nullable = false)
    private String busNumber;

    @Column(name = "route", nullable = false)
    private String route;

    @Column(name = "scheduled_duration_minutes", nullable = false)
    private Integer scheduledDurationMinutes;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    private String status; // 'Applied', 'Under Review', 'Waived'

    @Column(name = "log_date", nullable = false)
    private String date; // yyyy-MM-dd

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
