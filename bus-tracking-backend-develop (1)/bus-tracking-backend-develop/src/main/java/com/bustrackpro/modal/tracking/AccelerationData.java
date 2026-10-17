package com.bustrackpro.modal.tracking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity to persist individual three-axis acceleration values associated with a GPS log.
 */
@Entity
@Table(name = "acceleration_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccelerationData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "gps_log_id", nullable = false)
    @JsonIgnore
    private GpsLog gpsLog;

    private Double x;
    private Double y;
    private Double z;
}
