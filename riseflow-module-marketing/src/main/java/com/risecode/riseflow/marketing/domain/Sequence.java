package com.risecode.riseflow.marketing.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sequences")
public class Sequence extends TenantAwareEntity {

    @NotNull
    @Column(name = "campaign_id", nullable = false, updatable = false)
    private UUID campaignId;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;
}
