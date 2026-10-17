package com.bustrackpro.dto.response.finance;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in the finance summary table — aggregated per bus for the requested period.
 * Weekday/weekend breakdown is derived from ScheduleAssignment.assignmentDate day-of-week.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusFinanceSummaryItem {

    private Integer busId;
    private String busNumber;
    private String owner;
    private Integer ownerId;

    /** Sum of permit fees for weekday assignments (Mon–Fri). */
    private BigDecimal weekdayFee;

    /** Sum of permit fees for weekend assignments (Sat–Sun). */
    private BigDecimal weekendFee;

    /** Sum of permit fees tagged as holiday. */
    private BigDecimal holidayFee;

    /** Total permit fees (weekday + weekend + holiday). */
    private BigDecimal permitFeeTotal;

    /** Total off-peak commissions. */
    private BigDecimal commissionTotal;

    /** Total penalties applied. */
    private BigDecimal penaltyTotal;

    /** Grand total = permitFeeTotal + commissionTotal. */
    private BigDecimal grandTotal;
}
