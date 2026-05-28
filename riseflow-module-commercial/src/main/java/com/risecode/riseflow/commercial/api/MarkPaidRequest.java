package com.risecode.riseflow.commercial.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MarkPaidRequest(@NotBlank String paymentMethod, @NotNull @Positive BigDecimal amount) {
}
