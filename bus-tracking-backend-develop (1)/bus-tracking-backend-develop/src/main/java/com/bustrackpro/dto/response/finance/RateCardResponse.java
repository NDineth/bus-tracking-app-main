package com.bustrackpro.dto.response.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the current rate card — returned to both Admin and BusOwner.
 * Structure mirrors the frontend financeService expectations exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCardResponse {

    private PermitFeeSection permitFee;
    private CommissionSection offPeakCommission;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;

    /**
     * Permit fee values per day type.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermitFeeSection {
        private BigDecimal weekday;
        private BigDecimal weekend;
        private BigDecimal holiday;
    }

    /**
     * Off-peak commission configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionSection {
        private String mode;
        private BigDecimal value;
        private LocalDate effectiveFrom;
    }
}
