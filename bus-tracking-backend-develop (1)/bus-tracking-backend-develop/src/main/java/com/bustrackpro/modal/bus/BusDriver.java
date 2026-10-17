package com.bustrackpro.modal.bus;

import com.bustrackpro.modal.user.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bus driver entity.
 */
@Entity
@Table(name = "bus_driver")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusDriver {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String licenseNumber;

    @OneToOne
    private Users users;

}
