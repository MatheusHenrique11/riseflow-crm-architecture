package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.ActionType;
import java.util.UUID;

public record SequenceStepResponse(
        UUID id, UUID sequenceId, int stepOrder, int delayMinutes,
        ActionType actionType, UUID templateId, String messageBody) {
}
