package com.risecode.riseflow.commercial.events;

import java.math.BigDecimal;
import java.util.UUID;

public class InvoicePaidEvent {

    private final UUID invoiceId;
    private final UUID tenantId;
    private final UUID dealId;
    private final BigDecimal totalAmount;

    public InvoicePaidEvent(UUID invoiceId, UUID tenantId, UUID dealId, BigDecimal totalAmount) {
        this.invoiceId = invoiceId;
        this.tenantId = tenantId;
        this.dealId = dealId;
        this.totalAmount = totalAmount;
    }

    public UUID invoiceId() { return invoiceId; }
    public UUID tenantId() { return tenantId; }
    public UUID dealId() { return dealId; }
    public BigDecimal totalAmount() { return totalAmount; }
}
