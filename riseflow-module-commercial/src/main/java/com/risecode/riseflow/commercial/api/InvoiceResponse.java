package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID accountId,
        UUID dealId,
        UUID proposalId,
        String invoiceNumber,
        InvoiceStatus status,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        String paymentMethod,
        String stripeInvoiceId,
        String stripePaymentIntentId,
        Instant paidAt,
        List<InvoiceItemResponse> items,
        Instant createdAt) {
}
