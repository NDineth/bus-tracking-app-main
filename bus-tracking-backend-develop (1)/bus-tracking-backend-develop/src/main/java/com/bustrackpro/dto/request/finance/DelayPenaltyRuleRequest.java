package com.bustrackpro.dto.request.finance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DelayPenaltyRuleRequest {

    @NotNull(message = "Penalty level is required")
    @Min(value = 1, message = "Penalty level must be at least 1")
    private Integer penaltyLevel;

    @NotNull(message = "Minimum delay minutes is required")
    @Min(value = 1, message = "Minimum delay minutes must be at least 1")
    private Integer minDelayMinutes;

    private BigDecimal fineAmount;
}
