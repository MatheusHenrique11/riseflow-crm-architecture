package com.risecode.riseflow.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(0) int page,
        @Min(1) @Max(200) int size,
        String sort
) {
}
