package com.risecode.riseflow.marketing.api;

import jakarta.validation.constraints.NotBlank;

public record CampaignRequest(@NotBlank String name, String description) {
}
