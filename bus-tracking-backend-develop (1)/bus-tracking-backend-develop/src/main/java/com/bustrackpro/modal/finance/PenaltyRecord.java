package com.bustrackpro.modal.finance;

import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.user.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a penalty and/or suspension manually applied to a bus by an admin/authority.
 */
@Entity
@Table(name = "penalty_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @ManyToOne
    @JoinColumn(name = "issued_by", nullable = false)
    private Users issuedBy;

    @Column(name = "penalty_amount", precision = 10, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "schedules_suspended")
    private Integer schedulesSuspended;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
