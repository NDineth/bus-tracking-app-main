package com.bustrackpro.repository;

import com.bustrackpro.modal.route.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Bus route repository.
 */
@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, Integer> {
}
