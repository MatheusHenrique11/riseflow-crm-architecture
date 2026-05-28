package com.risecode.riseflow.deals.api;

import java.util.List;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        boolean defaultPipeline,
        List<StageResponse> stages) {
}
