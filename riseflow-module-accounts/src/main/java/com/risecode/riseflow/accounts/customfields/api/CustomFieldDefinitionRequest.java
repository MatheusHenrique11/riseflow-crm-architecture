package com.risecode.riseflow.accounts.customfields.api;

import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CustomFieldDefinitionRequest(
        @NotBlank @Size(max = 60) String entityType,
        @NotBlank @Size(max = 120) String fieldName,
        @NotNull FieldType fieldType,
        List<String> picklistOptions,
        boolean required,
        int displayOrder
) {
}
