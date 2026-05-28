package com.risecode.riseflow.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptTenantWithValidData() {
        Tenant tenant = validTenant();

        assertThat(validator.validate(tenant)).isEmpty();
    }

    @Test
    void shouldRejectTenantWhenNameIsBlank() {
        Tenant tenant = validTenant();
        tenant.setName(" ");

        assertThat(validator.validate(tenant)).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void shouldRejectTenantWhenDomainIsInvalid() {
        Tenant tenant = validTenant();
        tenant.setDomain("Invalid Domain!");

        assertThat(validator.validate(tenant)).anyMatch(v -> v.getPropertyPath().toString().equals("domain"));
    }

    @Test
    void shouldRejectTenantWhenOwnerEmailIsInvalid() {
        Tenant tenant = validTenant();
        tenant.setOwnerEmail("not-an-email");

        assertThat(validator.validate(tenant)).anyMatch(v -> v.getPropertyPath().toString().equals("ownerEmail"));
    }

    private Tenant validTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("Acme Corp");
        tenant.setDomain("acme-corp");
        tenant.setOwnerEmail("owner@acme.test");
        return tenant;
    }
}
