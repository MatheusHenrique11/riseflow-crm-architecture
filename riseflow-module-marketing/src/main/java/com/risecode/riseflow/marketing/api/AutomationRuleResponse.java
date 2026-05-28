package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.TriggerType;
import java.util.Map;
import java.util.UUID;

public record AutomationRuleResponse(
        UUID id, UUID tenantId, String name, TriggerType triggerType,
        Map<String, String> triggerConfig, UUID sequenceId, boolean active) {
}
