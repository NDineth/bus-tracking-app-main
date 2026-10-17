package com.bustrackpro.modal.tracking;

import com.bustrackpro.modal.bus.Bus;
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
 * Entity to persist every driving status event received from the IoT Hub.
 */
@Entity
@Table(name = "driving_status_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrivingStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Bus bus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrivingBehaviorType status;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
