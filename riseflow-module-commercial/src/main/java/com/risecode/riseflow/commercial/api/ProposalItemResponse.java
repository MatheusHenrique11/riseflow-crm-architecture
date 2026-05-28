package com.risecode.riseflow.commercial.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ProposalItemResponse(
        UUID id,
        UUID proposalId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        int orderNo) {
}
