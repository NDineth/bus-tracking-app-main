package com.bustrackpro.modal.schedule;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.route.BusRoute;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bus schedule entity.
 */
@Entity
@Table(name = "bus_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Bus bus;

    @ManyToOne
    private BusRoute busRoute;

    private LocalDateTime departureTime;

    private LocalDateTime estimatedArrivalTime;

    private String status;
}
