package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.ProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProposalResponse(
        UUID id,
        UUID tenantId,
        UUID accountId,
        UUID dealId,
        String title,
        String description,
        ProposalStatus status,
        LocalDate validUntil,
        BigDecimal totalValue,
        List<ProposalItemResponse> items,
        Map<String, Object> customFields,
        UUID createdBy,
        Instant createdAt) {
}
