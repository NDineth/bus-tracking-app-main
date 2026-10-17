package com.bustrackpro.modal.route;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bus route halt entity.
 */
@Entity
@Table(name = "bus_route_halt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRouteHalt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private BusRoute busRoute;

    @ManyToOne
    private BusHalt busHalt;

    private int stopOrder;
}
