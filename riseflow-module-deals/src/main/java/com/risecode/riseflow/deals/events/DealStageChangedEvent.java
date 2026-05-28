package com.risecode.riseflow.deals.events;

import java.util.UUID;

public class DealStageChangedEvent {

    private final UUID dealId;
    private final UUID tenantId;
    private final UUID oldStageId;
    private final UUID newStageId;
    private final UUID accountId;
    private final String accountEmail;

    public DealStageChangedEvent(UUID dealId, UUID tenantId, UUID oldStageId, UUID newStageId,
            UUID accountId, String accountEmail) {
        this.dealId = dealId;
        this.tenantId = tenantId;
        this.oldStageId = oldStageId;
        this.newStageId = newStageId;
        this.accountId = accountId;
        this.accountEmail = accountEmail;
    }

    public UUID dealId() { return dealId; }
    public UUID tenantId() { return tenantId; }
    public UUID oldStageId() { return oldStageId; }
    public UUID newStageId() { return newStageId; }
    public UUID accountId() { return accountId; }
    public String accountEmail() { return accountEmail; }
}
