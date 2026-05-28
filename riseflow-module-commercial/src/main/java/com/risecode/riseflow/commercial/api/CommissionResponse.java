package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.CommissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionResponse(
        UUID id,
        UUID tenantId,
        UUID dealId,
        UUID userId,
        UUID proposalId,
        UUID invoiceId,
        BigDecimal percentage,
        BigDecimal amount,
        CommissionStatus status,
        Instant paidAt,
        Instant createdAt) {
}
