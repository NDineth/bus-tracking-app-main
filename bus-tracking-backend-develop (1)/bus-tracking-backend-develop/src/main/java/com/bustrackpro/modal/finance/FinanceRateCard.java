package com.bustrackpro.modal.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Rate Card entity — persists permit fee and commission rates in the database.
 * Using the Rate Card pattern: rates are never hardcoded in business logic.
 * Admins update this entity; calculation services always read the latest entry.
 */
@Entity
@Table(name = "finance_rate_card")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceRateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    /** Permit fee per trip on a weekday (Mon–Fri). */
    @Column(name = "weekday_permit_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal weekdayPermitFee;

    /** Permit fee per trip on a weekend (Sat–Sun). */
    @Column(name = "weekend_permit_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal weekendPermitFee;

    /** Permit fee per trip on a public holiday. */
    @Column(name = "holiday_permit_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal holidayPermitFee;

    /**
     * Commission mode: "PERCENTAGE" applies value as % of revenue;
     * "FLAT" applies a fixed amount per off-peak trip.
     */
    @Column(name = "off_peak_commission_mode", nullable = false)
    private String offPeakCommissionMode;

    /** The numeric commission value (percentage or flat amount). */
    @Column(name = "off_peak_commission_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal offPeakCommissionValue;

    /** Date from which this rate card takes effect. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /** Email of the admin who last updated the rates. */
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
