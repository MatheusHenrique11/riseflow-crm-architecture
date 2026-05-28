package com.risecode.riseflow.accounts.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SearchCriteria(
        @NotBlank String fieldName,
        @NotNull SearchOperator operator,
        String value
) {
}
