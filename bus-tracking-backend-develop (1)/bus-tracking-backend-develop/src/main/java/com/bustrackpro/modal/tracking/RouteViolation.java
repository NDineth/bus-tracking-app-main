package com.bustrackpro.modal.tracking;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Route violation entity — records each detected compliance violation.
 */
@Entity
@Table(name = "route_violation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Bus bus;

    @ManyToOne
    private ScheduleAssignment scheduleAssignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;

    private String description;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
