package com.risecode.riseflow.core.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.risecode.riseflow.core.exception.TenantMismatchException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReturnCurrentTenantWhenTenantIdWasSet() {
        UUID tenantId = UUID.randomUUID();

        TenantContextHolder.setTenantId(tenantId);

        assertThat(TenantContextHolder.requireTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldThrowExceptionWhenTenantIdMissing() {
        assertThatThrownBy(TenantContextHolder::requireTenantId)
                .isInstanceOf(TenantMismatchException.class)
                .hasMessageContaining("Tenant id is required");
    }
}
