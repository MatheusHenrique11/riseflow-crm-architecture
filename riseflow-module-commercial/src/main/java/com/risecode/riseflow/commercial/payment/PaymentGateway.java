package com.risecode.riseflow.commercial.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult createInvoice(String customerEmail, String description, BigDecimal amount, String currency);

    WebhookEvent handleWebhook(String payload, String signature);
}
