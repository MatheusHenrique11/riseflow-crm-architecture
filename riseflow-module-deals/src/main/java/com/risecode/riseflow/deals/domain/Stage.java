package com.risecode.riseflow.deals.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stages")
public class Stage extends TenantAwareEntity {

    @NotNull
    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private UUID pipelineId;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int position;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private int probability;

    @Size(max = 10)
    @Column(length = 10)
    private String color;
}
