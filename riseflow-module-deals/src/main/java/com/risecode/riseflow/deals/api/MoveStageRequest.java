package com.risecode.riseflow.deals.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MoveStageRequest(@NotNull UUID stageId) {
}
