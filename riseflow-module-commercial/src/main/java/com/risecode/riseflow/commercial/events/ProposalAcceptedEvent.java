package com.risecode.riseflow.commercial.events;

import java.util.UUID;

public class ProposalAcceptedEvent {

    private final UUID proposalId;
    private final UUID tenantId;
    private final UUID accountId;
    private final UUID dealId;

    public ProposalAcceptedEvent(UUID proposalId, UUID tenantId, UUID accountId, UUID dealId) {
        this.proposalId = proposalId;
        this.tenantId = tenantId;
        this.accountId = accountId;
        this.dealId = dealId;
    }

    public UUID proposalId() { return proposalId; }
    public UUID tenantId() { return tenantId; }
    public UUID accountId() { return accountId; }
    public UUID dealId() { return dealId; }
}
