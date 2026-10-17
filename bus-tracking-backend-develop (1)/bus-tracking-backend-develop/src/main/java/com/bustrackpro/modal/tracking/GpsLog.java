package com.bustrackpro.modal.tracking;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * GPS log entity — stores every GPS ping received from a bus.
 */
@Entity
@Table(name = "gps_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Bus bus;

    @ManyToOne
    private ScheduleAssignment scheduleAssignment;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double speed;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @OneToMany(mappedBy = "gpsLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccelerationData> acceleration;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
