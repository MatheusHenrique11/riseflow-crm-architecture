package com.risecode.riseflow.commercial.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class Invoice extends TenantAwareEntity {

    @NotNull
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "deal_id")
    private UUID dealId;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Size(max = 60)
    @Column(name = "payment_method", length = 60)
    private String paymentMethod;

    @Size(max = 120)
    @Column(name = "stripe_invoice_id", length = 120)
    private String stripeInvoiceId;

    @Size(max = 120)
    @Column(name = "stripe_payment_intent_id", length = 120)
    private String stripePaymentIntentId;

    @Column(name = "paid_at")
    private Instant paidAt;
}
