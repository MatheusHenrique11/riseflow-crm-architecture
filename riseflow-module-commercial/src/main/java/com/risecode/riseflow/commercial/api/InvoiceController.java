package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.InvoiceStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_invoice:read')")
    List<InvoiceResponse> list(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) InvoiceStatus status) {
        return invoiceService.listInvoices(accountId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_invoice:read')")
    InvoiceResponse get(@PathVariable UUID id) {
        return invoiceService.getInvoice(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    InvoiceResponse update(@PathVariable UUID id, @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.updateInvoice(id, request);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    InvoiceResponse pay(@PathVariable UUID id, @Valid @RequestBody MarkPaidRequest request) {
        return invoiceService.markAsPaid(id, request);
    }

    @PostMapping("/{id}/create-payment")
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    CreatePaymentResponse createPayment(@PathVariable UUID id) {
        return invoiceService.createPayment(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    InvoiceResponse cancel(@PathVariable UUID id) {
        return invoiceService.cancelInvoice(id);
    }

    @PostMapping("/webhook")
    ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        invoiceService.processPaymentWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
