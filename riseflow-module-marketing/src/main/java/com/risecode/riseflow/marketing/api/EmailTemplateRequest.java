package com.risecode.riseflow.marketing.api;

import jakarta.validation.constraints.NotBlank;

public record EmailTemplateRequest(
        @NotBlank String name,
        @NotBlank String subject,
        @NotBlank String body) {
}
