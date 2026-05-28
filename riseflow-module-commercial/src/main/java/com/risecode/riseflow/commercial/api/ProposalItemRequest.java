package com.risecode.riseflow.commercial.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProposalItemRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        int orderNo) {
}
