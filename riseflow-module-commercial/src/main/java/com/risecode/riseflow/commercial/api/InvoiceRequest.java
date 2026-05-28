package com.risecode.riseflow.commercial.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        @NotNull UUID accountId,
        UUID dealId,
        UUID proposalId,
        String invoiceNumber,
        @NotNull LocalDate dueDate,
        @NotEmpty @Valid List<InvoiceItemRequest> items) {
}
