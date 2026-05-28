package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.CampaignStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignStatusRequest(@NotNull CampaignStatus status) {
}
