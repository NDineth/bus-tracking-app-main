package com.bustrackpro.repository;

import com.bustrackpro.modal.route.BusRoute;
import com.bustrackpro.modal.route.BusRouteHalt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Bus route halt repository.
 */
@Repository
public interface BusRouteHaltRepository extends JpaRepository<BusRouteHalt, Integer> {

    List<BusRouteHalt> findByBusRoute(BusRoute busRoute);

    /**
     * Find all halts for a route, ordered by stop sequence.
     *
     * @param busRoute - the route
     * @return ordered list of route halts
     */
    List<BusRouteHalt> findByBusRouteOrderByStopOrderAsc(BusRoute busRoute);

    void deleteByBusRoute(BusRoute busRoute);

}
