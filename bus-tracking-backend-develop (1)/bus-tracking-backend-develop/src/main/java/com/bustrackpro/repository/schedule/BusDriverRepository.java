package com.bustrackpro.repository.schedule;

import com.bustrackpro.modal.bus.BusDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Bus driver repository.
 */
@Repository
public interface BusDriverRepository extends JpaRepository<BusDriver, Integer> {
    java.util.Optional<BusDriver> findByUsers(com.bustrackpro.modal.user.Users users);
}
