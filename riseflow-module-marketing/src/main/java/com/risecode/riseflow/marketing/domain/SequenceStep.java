package com.risecode.riseflow.marketing.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sequence_steps")
public class SequenceStep extends TenantAwareEntity {

    @NotNull
    @Column(name = "sequence_id", nullable = false, updatable = false)
    private UUID sequenceId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Min(0)
    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;
}
