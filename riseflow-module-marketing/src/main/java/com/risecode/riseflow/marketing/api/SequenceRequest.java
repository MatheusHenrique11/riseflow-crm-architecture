package com.risecode.riseflow.marketing.api;

import jakarta.validation.constraints.NotBlank;

public record SequenceRequest(@NotBlank String name) {
}
