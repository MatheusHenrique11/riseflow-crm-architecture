package com.risecode.riseflow.multitenancy;

import com.risecode.riseflow.core.tenant.TenantContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContextHolder.getTenantId()
                .map(Object::toString)
                .orElse("public");
    }
}
