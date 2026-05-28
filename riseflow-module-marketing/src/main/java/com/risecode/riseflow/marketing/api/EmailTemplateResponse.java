package com.risecode.riseflow.marketing.api;

import java.util.UUID;

public record EmailTemplateResponse(UUID id, UUID tenantId, String name, String subject, String body) {
}
