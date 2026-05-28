package com.risecode.riseflow.deals.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PipelineRequest(
        @NotBlank String name,
        String description,
        boolean defaultPipeline,
        @Valid List<StageRequest> stages) {
}
