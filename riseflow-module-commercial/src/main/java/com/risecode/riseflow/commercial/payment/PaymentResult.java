package com.risecode.riseflow.commercial.payment;

public record PaymentResult(
        String gatewayInvoiceId,
        String paymentIntentId,
        String clientSecret,
        String status) {
}
