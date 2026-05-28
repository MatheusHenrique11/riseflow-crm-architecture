package com.risecode.riseflow.deals.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "deals")
public class Deal extends TenantAwareEntity {

    @Column(name = "account_id")
    private UUID accountId;

    @NotNull
    @Column(name = "pipeline_id", nullable = false)
    private UUID pipelineId;

    @NotNull
    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Size(max = 3)
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false)
    private int probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    @Column(name = "responsible_id")
    private UUID responsibleId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DealStatus status = DealStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> customFields = new HashMap<>();
}
