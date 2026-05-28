package com.risecode.riseflow.deals.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StageRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer position,
        @Min(0) @Max(100) int probability,
        String color) {
}
