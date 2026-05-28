package com.risecode.riseflow.accounts.api;

import java.util.Map;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID tenantId,
        String name,
        String industry,
        String email,
        String phone,
        Map<String, Object> customFields
) {
}
