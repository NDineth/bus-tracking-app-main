package com.bustrackpro.dto.response.finance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PenaltyRecordResponse {
    private Integer id;
    private String busNumber;
    private String issuedByName;
    private BigDecimal penaltyAmount;
    private Integer schedulesSuspended;
    private String reason;
    private LocalDateTime createdAt;
}
