package com.bustrackpro.dto.response.finance;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCalculationResult {

    private LocalTime time;
    private Double distanceKm;
    private Double rateApplied;
    private String type; // TOLL, SUBSIDY, or NONE
    private Double amount;
    private Integer delayMinutes;
    private Double delayPenalty;

}
