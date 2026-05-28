package com.risecode.riseflow.deals.events;

import java.math.BigDecimal;
import java.util.UUID;

public class DealWonEvent {

    private final UUID dealId;
    private final UUID tenantId;
    private final UUID accountId;
    private final UUID responsibleId;
    private final BigDecimal amount;
    private final String currency;

    public DealWonEvent(UUID dealId, UUID tenantId, UUID accountId, UUID responsibleId,
            BigDecimal amount, String currency) {
        this.dealId = dealId;
        this.tenantId = tenantId;
        this.accountId = accountId;
        this.responsibleId = responsibleId;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID dealId() { return dealId; }
    public UUID tenantId() { return tenantId; }
    public UUID accountId() { return accountId; }
    public UUID responsibleId() { return responsibleId; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
}
