package com.risecode.riseflow.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRoutingDataSourceTest {
    private final ExposedTenantRoutingDataSource dataSource = new ExposedTenantRoutingDataSource();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldRouteToPublicSchemaWhenTenantIsMissing() {
        assertThat(dataSource.lookupKey()).isEqualTo("public");
    }

    @Test
    void shouldRouteToCurrentTenantWhenTenantIsPresent() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        assertThat(dataSource.lookupKey()).isEqualTo(tenantId.toString());
    }

    private static final class ExposedTenantRoutingDataSource extends TenantRoutingDataSource {
        Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
