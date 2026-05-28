package com.risecode.riseflow.deals.api;

import java.util.UUID;

public record StageResponse(
        UUID id,
        UUID tenantId,
        UUID pipelineId,
        String name,
        int position,
        int probability,
        String color) {
}
