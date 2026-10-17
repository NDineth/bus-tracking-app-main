package com.bustrackpro.repository.finance;

import com.bustrackpro.modal.finance.PenaltyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

@Repository
public interface PenaltyRecordRepository extends JpaRepository<PenaltyRecord, Integer> {
    List<PenaltyRecord> findByBusIdOrderByCreatedAtDesc(Integer busId);
    List<PenaltyRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
