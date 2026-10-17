package com.bustrackpro.dto.response.finance;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySettlementResponse {

    private Integer busId;
    private LocalDate date;
    private Double totalTollsPaid;
    private Double totalSubsidiesReceived;
    private Double totalDelayPenalties;
    private Double netPayment;
    private String paymentStatus; // OWED_TO_GOVERNMENT, RECEIVED_FROM_GOVERNMENT, ZERO_BALANCE
    private List<TripCalculationResult> tripCalculations;

}
