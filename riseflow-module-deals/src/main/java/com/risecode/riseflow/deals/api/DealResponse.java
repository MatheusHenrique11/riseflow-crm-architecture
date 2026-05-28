package com.risecode.riseflow.deals.api;

import com.risecode.riseflow.deals.domain.DealStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record DealResponse(
        UUID id,
        UUID tenantId,
        UUID pipelineId,
        UUID stageId,
        UUID accountId,
        String title,
        BigDecimal amount,
        String currency,
        int probability,
        DealStatus status,
        LocalDate expectedCloseDate,
        LocalDate actualCloseDate,
        UUID responsibleId,
        String notes,
        Map<String, Object> customFields) {
}
