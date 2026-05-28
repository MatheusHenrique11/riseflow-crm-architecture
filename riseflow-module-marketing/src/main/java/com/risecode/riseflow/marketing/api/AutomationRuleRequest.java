package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record AutomationRuleRequest(
        @NotBlank String name,
        @NotNull TriggerType triggerType,
        Map<String, String> triggerConfig,
        @NotNull UUID sequenceId,
        boolean active) {
}
