package com.bustrackpro.repository.finance;

import com.bustrackpro.modal.finance.DelayLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DelayLogRecordRepository extends JpaRepository<DelayLogRecord, Integer> {

    List<DelayLogRecord> findByDateBetween(String fromDate, String toDate);

    List<DelayLogRecord> findByBusNumberContainingIgnoreCase(String busNumber);
}
