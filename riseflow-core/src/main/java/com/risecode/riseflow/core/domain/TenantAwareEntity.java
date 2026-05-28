package com.risecode.riseflow.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {
    @NotNull
    @Column(nullable = false, updatable = false)
    private UUID tenantId;
}
