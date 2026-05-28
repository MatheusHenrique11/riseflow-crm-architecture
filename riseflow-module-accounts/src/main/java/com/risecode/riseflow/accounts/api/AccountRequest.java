package com.risecode.riseflow.accounts.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AccountRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 160) String industry,
        @Email @Size(max = 180) String email,
        @Size(max = 40) String phone,
        Map<String, Object> customFields
) {
}
