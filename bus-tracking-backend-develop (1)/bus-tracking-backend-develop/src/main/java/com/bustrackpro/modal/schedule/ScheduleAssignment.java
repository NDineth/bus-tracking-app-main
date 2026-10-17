package com.bustrackpro.modal.schedule;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.bus.BusDriver;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Schedule assignment entity mapping a trip to a specific bus and driver on a date.
 */
@Entity
@Table(
    name = "schedule_assignment",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trip_id", "assignment_date"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Trip trip;

    @ManyToOne
    private Bus bus;

    @ManyToOne
    private BusDriver driver;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.SCHEDULED;

    private String notes;

    @Column(name = "permit_fee")
    private BigDecimal permitFee;

    @Column(name = "off_peak_commission")
    private BigDecimal offPeakCommission;

    @Column(name = "is_off_peak")
    private Boolean isOffPeak;

    @Column(name = "delay_minutes")
    private Integer delayMinutes = 0;

    @Column(name = "fine_amount")
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "penalty_level")
    private Integer penaltyLevel = 0;

    @Column(name = "actual_distance")
    private Double actualDistance;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
