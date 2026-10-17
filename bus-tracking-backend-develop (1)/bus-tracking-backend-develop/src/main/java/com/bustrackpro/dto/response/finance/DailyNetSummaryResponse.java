package com.bustrackpro.dto.response.finance;

import com.bustrackpro.dto.response.schedule.ScheduleAssignmentResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the daily net permit and commission summary of a bus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNetSummaryResponse {

    private String busNumber;
    private LocalDate date;
    private int totalRuns;
    private BigDecimal totalPermitFees;
    private BigDecimal totalCommissions;
    private BigDecimal totalFines;
    private BigDecimal dailyNetRevenue;
    private List<ScheduleAssignmentResponse> runs;
}
