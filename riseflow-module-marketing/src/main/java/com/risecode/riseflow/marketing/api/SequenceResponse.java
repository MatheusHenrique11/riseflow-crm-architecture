package com.risecode.riseflow.marketing.api;

import java.util.List;
import java.util.UUID;

public record SequenceResponse(UUID id, UUID tenantId, UUID campaignId, String name, List<SequenceStepResponse> steps) {
}
