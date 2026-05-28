package com.risecode.riseflow.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptAccountWithValidData() {
        assertThat(validator.validate(validAccount())).isEmpty();
    }

    @Test
    void shouldRejectAccountWhenTenantIdMissing() {
        Account account = validAccount();
        account.setTenantId(null);

        assertThat(validator.validate(account)).anyMatch(v -> v.getPropertyPath().toString().equals("tenantId"));
    }

    @Test
    void shouldRejectAccountWhenNameIsBlank() {
        Account account = validAccount();
        account.setName(" ");

        assertThat(validator.validate(account)).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void shouldRejectAccountWhenEmailIsInvalid() {
        Account account = validAccount();
        account.setEmail("bad-email");

        assertThat(validator.validate(account)).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    private Account validAccount() {
        Account account = new Account();
        account.setTenantId(UUID.randomUUID());
        account.setName("Acme Corp");
        account.setIndustry("Software");
        account.setEmail("hello@acme.test");
        account.setPhone("+15551234567");
        return account;
    }
}
