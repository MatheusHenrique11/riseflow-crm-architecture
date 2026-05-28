package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.ActionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SequenceStepRequest(
        @NotNull @Min(1) Integer stepOrder,
        @Min(0) int delayMinutes,
        @NotNull ActionType actionType,
        UUID templateId,
        String messageBody) {
}
