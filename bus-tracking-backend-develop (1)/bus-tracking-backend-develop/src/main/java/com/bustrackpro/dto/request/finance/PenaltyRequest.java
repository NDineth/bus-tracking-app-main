package com.bustrackpro.dto.request.finance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PenaltyRequest {

    @NotNull(message = "Bus ID is required")
    private Integer busId;

    @NotNull(message = "Penalty amount is required")
    @Min(value = 0, message = "Penalty amount must be zero or greater")
    private BigDecimal penaltyAmount;

    @NotNull(message = "Schedules to suspend is required")
    @Min(value = 0, message = "Schedules to suspend must be zero or greater")
    private Integer schedulesSuspended;

    private String reason;
}
