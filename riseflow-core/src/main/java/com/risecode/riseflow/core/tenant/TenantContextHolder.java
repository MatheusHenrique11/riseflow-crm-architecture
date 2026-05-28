package com.risecode.riseflow.core.tenant;

import com.risecode.riseflow.core.exception.TenantMismatchException;
import java.util.Optional;
import java.util.UUID;

public final class TenantContextHolder {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID requireTenantId() {
        return getTenantId().orElseThrow(() -> new TenantMismatchException("Tenant id is required"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
