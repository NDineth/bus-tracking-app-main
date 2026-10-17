package com.bustrackpro.dto.request.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request payload for updating the rate card (Admin only).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateCardUpdateRequest {

    /** Permit fee section. */
    private PermitFeeSection permitFee;

    /** Off-peak commission section. */
    private CommissionSection offPeakCommission;

    /**
     * Permit fee rates per day type.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermitFeeSection {
        private BigDecimal weekday;
        private BigDecimal weekend;
        private BigDecimal holiday;
    }

    /**
     * Off-peak commission settings.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionSection {
        /** "percentage" or "flat". */
        private String mode;
        private BigDecimal value;
        private LocalDate effectiveFrom;
    }
}
