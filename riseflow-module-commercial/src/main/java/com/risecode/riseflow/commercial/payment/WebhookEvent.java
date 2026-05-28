package com.risecode.riseflow.commercial.payment;

import java.math.BigDecimal;

public record WebhookEvent(
        String type,
        String gatewayInvoiceId,
        String status,
        BigDecimal amount) {
}
