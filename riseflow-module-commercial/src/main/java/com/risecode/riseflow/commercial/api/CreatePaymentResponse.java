package com.risecode.riseflow.commercial.api;

public record CreatePaymentResponse(
        String gatewayInvoiceId,
        String paymentIntentId,
        String clientSecret,
        String status) {
}
