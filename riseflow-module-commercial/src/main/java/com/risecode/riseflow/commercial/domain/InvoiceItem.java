package com.risecode.riseflow.commercial.domain;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends TenantAwareEntity {

    @NotNull
    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String description;

    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Positive
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "order_no", nullable = false)
    private int orderNo;
}
