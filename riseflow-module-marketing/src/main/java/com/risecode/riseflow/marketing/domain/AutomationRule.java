package com.risecode.riseflow.marketing.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "automation_rules")
public class AutomationRule extends TenantAwareEntity {

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 40)
    private TriggerType triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private Map<String, String> triggerConfig;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActionRuleType actionType = ActionRuleType.ENROLL_IN_SEQUENCE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> actionConfig;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public enum ActionRuleType {
        ENROLL_IN_SEQUENCE
    }
}
