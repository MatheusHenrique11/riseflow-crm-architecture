package com.risecode.riseflow.marketing.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sequence_enrollments")
public class SequenceEnrollment extends TenantAwareEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @NotNull
    @Column(name = "sequence_id", nullable = false)
    private UUID sequenceId;

    @Column(name = "current_step_order", nullable = false)
    private int currentStepOrder = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "next_execution_time")
    private Instant nextExecutionTime;
}
