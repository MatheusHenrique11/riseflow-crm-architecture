package com.risecode.riseflow.accounts.customfields.api;

import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id,
        UUID tenantId,
        String entityType,
        String fieldName,
        FieldType fieldType,
        List<String> picklistOptions,
        boolean required,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
