package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.Invoice;
import com.risecode.riseflow.commercial.domain.InvoiceItem;
import com.risecode.riseflow.commercial.domain.InvoiceStatus;
import com.risecode.riseflow.commercial.events.InvoicePaidEvent;
import com.risecode.riseflow.commercial.payment.PaymentGateway;
import com.risecode.riseflow.commercial.payment.PaymentResult;
import com.risecode.riseflow.commercial.payment.WebhookEvent;
import com.risecode.riseflow.commercial.persistence.InvoiceItemRepository;
import com.risecode.riseflow.commercial.persistence.InvoiceRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceService(InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            PaymentGateway paymentGateway,
            ApplicationEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        String invoiceNumber = resolveInvoiceNumber(request.invoiceNumber(), tenantId);

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setAccountId(request.accountId());
        invoice.setDealId(request.dealId());
        invoice.setProposalId(request.proposalId());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDueDate(request.dueDate());
        invoice.setTotalAmount(BigDecimal.ZERO);
        Invoice saved = invoiceRepository.save(invoice);

        List<InvoiceItem> items = saveItems(request.items(), saved.getId(), tenantId);
        saved.setTotalAmount(computeTotal(items));
        saved = invoiceRepository.save(saved);

        return toResponse(saved, items);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID accountId, InvoiceStatus status) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return invoiceRepository.findFiltered(tenantId, accountId, status)
                .stream().map(i -> toResponse(i, invoiceItemRepository.findAllByInvoiceIdOrderByOrderNo(i.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        Invoice invoice = findForTenant(id);
        return toResponse(invoice, invoiceItemRepository.findAllByInvoiceIdOrderByOrderNo(id));
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID id, InvoiceRequest request) {
        Invoice invoice = findForTenant(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("Only PENDING invoices can be updated");
        }
        invoice.setAccountId(request.accountId());
        invoice.setDealId(request.dealId());
        invoice.setDueDate(request.dueDate());

        invoiceItemRepository.deleteAllByInvoiceId(id);
        List<InvoiceItem> items = saveItems(request.items(), id, invoice.getTenantId());
        invoice.setTotalAmount(computeTotal(items));
        return toResponse(invoiceRepository.save(invoice), items);
    }

    @Transactional
    public InvoiceResponse markAsPaid(UUID id, MarkPaidRequest request) {
        Invoice invoice = findForTenant(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessException("Invoice cannot be marked as paid in status: " + invoice.getStatus());
        }
        invoice.setPaidAmount(request.amount());
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setPaidAt(Instant.now());
        if (request.amount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        Invoice saved = invoiceRepository.save(invoice);
        if (saved.getStatus() == InvoiceStatus.PAID) {
            eventPublisher.publishEvent(new InvoicePaidEvent(
                    saved.getId(), saved.getTenantId(), saved.getDealId(), saved.getTotalAmount()));
        }
        return toResponse(saved, invoiceItemRepository.findAllByInvoiceIdOrderByOrderNo(id));
    }

    @Transactional
    public CreatePaymentResponse createPayment(UUID id) {
        Invoice invoice = findForTenant(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessException("Cannot create payment for invoice in status: " + invoice.getStatus());
        }
        PaymentResult result = paymentGateway.createInvoice(
                "customer@example.com", "Invoice " + invoice.getInvoiceNumber(),
                invoice.getTotalAmount(), "USD");
        invoice.setStripeInvoiceId(result.gatewayInvoiceId());
        invoice.setStripePaymentIntentId(result.paymentIntentId());
        invoiceRepository.save(invoice);
        return new CreatePaymentResponse(result.gatewayInvoiceId(), result.paymentIntentId(),
                result.clientSecret(), result.status());
    }

    @Transactional
    public void processPaymentWebhook(String payload, String signature) {
        WebhookEvent event = paymentGateway.handleWebhook(payload, signature);
        if (!"invoice.paid".equals(event.type())) {
            log.info("Ignoring webhook event type: {}", event.type());
            return;
        }
        invoiceRepository.findAll().stream()
                .filter(i -> event.gatewayInvoiceId().equals(i.getStripeInvoiceId()))
                .findFirst()
                .ifPresentOrElse(invoice -> {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setPaidAmount(invoice.getTotalAmount());
                    invoice.setPaidAt(Instant.now());
                    Invoice saved = invoiceRepository.save(invoice);
                    eventPublisher.publishEvent(new InvoicePaidEvent(
                            saved.getId(), saved.getTenantId(), saved.getDealId(), saved.getTotalAmount()));
                }, () -> log.warn("No invoice found for gateway id: {}", event.gatewayInvoiceId()));
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID id) {
        Invoice invoice = findForTenant(id);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("Only PENDING invoices can be cancelled");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return toResponse(invoiceRepository.save(invoice), invoiceItemRepository.findAllByInvoiceIdOrderByOrderNo(id));
    }

    @Transactional
    public int markOverdueInvoices() {
        return invoiceRepository.markOverdueInvoices(LocalDate.now(), InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
    }

    // --- Private helpers ---

    private String resolveInvoiceNumber(String requested, UUID tenantId) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        long count = invoiceRepository.countByTenantId(tenantId);
        return String.format("INV-%d-%05d", LocalDate.now().getYear(), count + 1);
    }

    private List<InvoiceItem> saveItems(List<InvoiceItemRequest> requests, UUID invoiceId, UUID tenantId) {
        return requests.stream().map(r -> {
            InvoiceItem item = new InvoiceItem();
            item.setTenantId(tenantId);
            item.setInvoiceId(invoiceId);
            item.setDescription(r.description());
            item.setQuantity(r.quantity());
            item.setUnitPrice(r.unitPrice());
            item.setTotal(r.quantity().multiply(r.unitPrice()));
            item.setOrderNo(r.orderNo());
            return invoiceItemRepository.save(item);
        }).toList();
    }

    private BigDecimal computeTotal(List<InvoiceItem> items) {
        return items.stream().map(InvoiceItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice findForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Invoice tenant does not match");
        }
        return invoice;
    }

    InvoiceResponse toResponse(Invoice inv, List<InvoiceItem> items) {
        return new InvoiceResponse(inv.getId(), inv.getTenantId(), inv.getAccountId(), inv.getDealId(),
                inv.getProposalId(), inv.getInvoiceNumber(), inv.getStatus(), inv.getDueDate(),
                inv.getTotalAmount(), inv.getPaidAmount(), inv.getPaymentMethod(),
                inv.getStripeInvoiceId(), inv.getStripePaymentIntentId(), inv.getPaidAt(),
                items.stream().map(i -> new InvoiceItemResponse(i.getId(), i.getInvoiceId(),
                        i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getOrderNo()))
                        .toList(),
                inv.getCreatedAt());
    }
}
