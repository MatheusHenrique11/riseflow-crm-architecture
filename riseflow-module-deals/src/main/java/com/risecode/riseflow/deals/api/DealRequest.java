package com.risecode.riseflow.deals.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record DealRequest(
        @NotBlank String title,
        @NotNull UUID pipelineId,
        @NotNull UUID stageId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        LocalDate expectedCloseDate,
        UUID responsibleId,
        String notes,
        Map<String, Object> customFields) {
}
