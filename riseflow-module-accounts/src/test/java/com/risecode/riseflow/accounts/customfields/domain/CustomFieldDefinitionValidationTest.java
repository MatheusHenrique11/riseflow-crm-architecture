package com.risecode.riseflow.accounts.customfields.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomFieldDefinitionValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidDefinition() {
        assertThat(validator.validate(validDefinition())).isEmpty();
    }

    @Test
    void shouldRejectBlankEntityType() {
        CustomFieldDefinition def = validDefinition();
        def.setEntityType(" ");

        assertThat(validator.validate(def))
                .anyMatch(v -> v.getPropertyPath().toString().equals("entityType"));
    }

    @Test
    void shouldRejectBlankFieldName() {
        CustomFieldDefinition def = validDefinition();
        def.setFieldName("");

        assertThat(validator.validate(def))
                .anyMatch(v -> v.getPropertyPath().toString().equals("fieldName"));
    }

    @Test
    void shouldRejectNullFieldType() {
        CustomFieldDefinition def = validDefinition();
        def.setFieldType(null);

        assertThat(validator.validate(def))
                .anyMatch(v -> v.getPropertyPath().toString().equals("fieldType"));
    }

    @Test
    void shouldRejectNullTenantId() {
        CustomFieldDefinition def = validDefinition();
        def.setTenantId(null);

        assertThat(validator.validate(def))
                .anyMatch(v -> v.getPropertyPath().toString().equals("tenantId"));
    }

    private CustomFieldDefinition validDefinition() {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setTenantId(UUID.randomUUID());
        def.setEntityType("ACCOUNT");
        def.setFieldName("revenue");
        def.setFieldType(FieldType.NUMBER);
        def.setDisplayOrder(1);
        return def;
    }
}
