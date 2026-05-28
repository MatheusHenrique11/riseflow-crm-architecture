package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.EntityType;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id, UUID tenantId, EntityType entityType, UUID entityId,
        UUID sequenceId, int currentStepOrder, EnrollmentStatus status,
        String contactEmail, Instant nextExecutionTime) {
}
