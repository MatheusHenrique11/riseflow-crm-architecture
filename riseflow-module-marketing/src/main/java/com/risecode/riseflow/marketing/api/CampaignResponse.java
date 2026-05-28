package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.CampaignStatus;
import java.util.List;
import java.util.UUID;

public record CampaignResponse(
        UUID id, UUID tenantId, String name, String description,
        CampaignStatus status, List<SequenceResponse> sequences) {
}
