package com.risecode.riseflow.commercial.api;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID invoiceId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        int orderNo) {
}
