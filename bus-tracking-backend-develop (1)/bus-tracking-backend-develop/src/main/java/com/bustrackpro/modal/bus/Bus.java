package com.bustrackpro.modal.bus;

import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.user.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bus entity.
 */
@Entity
@Table(name = "bus")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String busNumber;

    private Integer deviceReferenceNumber;

    private Integer capacity;

    private String model;

    @OneToOne
    @JoinColumn(name = "driver_id", unique = true)
    private Users driver;
    
    @ManyToOne
    private Users owner;
    
    @ManyToOne
    private BusRoute route;
}
