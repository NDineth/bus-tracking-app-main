package com.bustrackpro.modal.route;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bus route entity.
 */
@Entity
@Table(name = "bus_route")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String routeNumber;

    private String startLocation;

    private String endLocation;

    private Double distance;
}
