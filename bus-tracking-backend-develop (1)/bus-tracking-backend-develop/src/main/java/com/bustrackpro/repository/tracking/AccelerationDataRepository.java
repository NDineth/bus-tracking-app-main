package com.bustrackpro.repository.tracking;

import com.bustrackpro.modal.tracking.AccelerationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for AccelerationData.
 */
@Repository
public interface AccelerationDataRepository extends JpaRepository<AccelerationData, Integer> {
}
