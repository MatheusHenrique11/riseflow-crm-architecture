package com.risecode.riseflow.commercial.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProposalRequest(
        @NotNull UUID accountId,
        UUID dealId,
        @NotBlank String title,
        String description,
        @NotNull LocalDate validUntil,
        @NotEmpty @Valid List<ProposalItemRequest> items,
        Map<String, Object> customFields) {
}
