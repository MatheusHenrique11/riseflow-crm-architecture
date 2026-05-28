package com.risecode.riseflow.core.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class RiseFlowEvent extends ApplicationEvent {
    private final UUID tenantId;
    private final Instant occurredAt;

    public RiseFlowEvent(Object source, UUID tenantId) {
        super(source);
        this.tenantId = tenantId;
        this.occurredAt = Instant.now();
    }

    public UUID tenantId() {
        return tenantId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
