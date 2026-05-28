package com.risecode.riseflow.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$", message = "domain must be a valid lowercase slug") String domain,
        @NotBlank @Email @Size(max = 180) String ownerEmail
) {
}
