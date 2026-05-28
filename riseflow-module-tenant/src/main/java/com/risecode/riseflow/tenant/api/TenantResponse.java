package com.risecode.riseflow.tenant.api;

import java.util.UUID;

public record TenantResponse(UUID id, String name, String domain, String ownerEmail, boolean active) {
}
