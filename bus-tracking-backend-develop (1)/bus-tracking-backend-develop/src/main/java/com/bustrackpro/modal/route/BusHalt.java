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
 * Bus halt entity.
 */
@Entity
@Table(name = "bus_halt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusHalt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String name;

    private Double latitude;

    private Double longitude;
}
