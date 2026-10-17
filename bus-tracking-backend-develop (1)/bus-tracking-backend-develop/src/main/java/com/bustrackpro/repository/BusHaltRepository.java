package com.bustrackpro.repository;

import com.bustrackpro.modal.route.BusHalt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Bus halt repository.
 */
@Repository
public interface BusHaltRepository extends JpaRepository<BusHalt, Integer> {
}
