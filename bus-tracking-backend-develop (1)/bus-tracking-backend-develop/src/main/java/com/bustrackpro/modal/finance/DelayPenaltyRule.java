package com.bustrackpro.modal.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a delay penalty configuration rule.
 */
@Entity
@Table(name = "delay_penalty_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayPenaltyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "penalty_level", nullable = false)
    private Integer penaltyLevel;

    @Column(name = "min_delay_minutes", nullable = false)
    private Integer minDelayMinutes;

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private BigDecimal fineAmount;
}
