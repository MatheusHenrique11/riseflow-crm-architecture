package com.risecode.riseflow.commercial.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "commissions")
public class Commission extends TenantAwareEntity {

    @NotNull
    @Column(name = "deal_id", nullable = false)
    private UUID dealId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;
}
