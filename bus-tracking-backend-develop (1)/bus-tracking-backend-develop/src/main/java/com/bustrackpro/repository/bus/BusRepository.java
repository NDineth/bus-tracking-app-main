package com.bustrackpro.repository.bus;

import com.bustrackpro.modal.bus.Bus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Bus repository.
 */
@Repository
public interface BusRepository extends JpaRepository<Bus, Integer> {
    java.util.Optional<Bus> findByBusNumber(String busNumber);

    java.util.Optional<Bus> findByDeviceReferenceNumber(Integer deviceReferenceNumber);

    /**
     * Find all buses owned by a specific user.
     *
     * @param ownerId - the owner's user id
     * @return list of buses
     */
    List<Bus> findByOwnerId(Integer ownerId);
}

