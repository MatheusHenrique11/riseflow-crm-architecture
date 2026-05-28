package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.Invoice;
import com.risecode.riseflow.commercial.domain.InvoiceStatus;
import com.risecode.riseflow.commercial.domain.Proposal;
import com.risecode.riseflow.commercial.domain.ProposalItem;
import com.risecode.riseflow.commercial.domain.ProposalStatus;
import com.risecode.riseflow.commercial.events.ProposalAcceptedEvent;
import com.risecode.riseflow.commercial.persistence.InvoiceItemRepository;
import com.risecode.riseflow.commercial.persistence.InvoiceRepository;
import com.risecode.riseflow.commercial.persistence.ProposalItemRepository;
import com.risecode.riseflow.commercial.persistence.ProposalRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalService {

    private static final BigDecimal DEFAULT_INVOICE_DAYS = BigDecimal.valueOf(30);

    private final ProposalRepository proposalRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProposalService(ProposalRepository proposalRepository,
            ProposalItemRepository proposalItemRepository,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            ApplicationEventPublisher eventPublisher) {
        this.proposalRepository = proposalRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProposalResponse createProposal(ProposalRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        validateValidUntil(request.validUntil());

        Proposal proposal = new Proposal();
        proposal.setTenantId(tenantId);
        proposal.setAccountId(request.accountId());
        proposal.setDealId(request.dealId());
        proposal.setTitle(request.title());
        proposal.setDescription(request.description());
        proposal.setValidUntil(request.validUntil());
        proposal.setCustomFields(request.customFields() != null ? request.customFields() : new java.util.HashMap<>());
        Proposal saved = proposalRepository.save(proposal);

        List<ProposalItem> items = saveItems(request.items(), saved.getId(), tenantId);
        BigDecimal total = computeTotal(items);
        saved.setTotalValue(total);
        saved = proposalRepository.save(saved);

        return toResponse(saved, items);
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> listProposals(UUID accountId, UUID dealId, ProposalStatus status) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return proposalRepository.findFiltered(tenantId, accountId, dealId, status)
                .stream().map(p -> toResponse(p, proposalItemRepository.findAllByProposalIdOrderByOrderNo(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProposalResponse getProposal(UUID id) {
        Proposal proposal = findForTenant(id);
        return toResponse(proposal, proposalItemRepository.findAllByProposalIdOrderByOrderNo(id));
    }

    @Transactional
    public ProposalResponse updateProposal(UUID id, ProposalRequest request) {
        Proposal proposal = findForTenant(id);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT proposals can be updated");
        }
        validateValidUntil(request.validUntil());

        proposal.setAccountId(request.accountId());
        proposal.setDealId(request.dealId());
        proposal.setTitle(request.title());
        proposal.setDescription(request.description());
        proposal.setValidUntil(request.validUntil());
        proposal.setCustomFields(request.customFields() != null ? request.customFields() : new java.util.HashMap<>());

        proposalItemRepository.deleteAllByProposalId(id);
        List<ProposalItem> items = saveItems(request.items(), id, proposal.getTenantId());
        proposal.setTotalValue(computeTotal(items));
        return toResponse(proposalRepository.save(proposal), items);
    }

    @Transactional
    public ProposalResponse changeStatus(UUID id, ProposalStatusRequest request) {
        Proposal proposal = findForTenant(id);
        validateStatusTransition(proposal.getStatus(), request.status());
        proposal.setStatus(request.status());
        Proposal saved = proposalRepository.save(proposal);
        if (request.status() == ProposalStatus.ACCEPTED) {
            eventPublisher.publishEvent(new ProposalAcceptedEvent(
                    saved.getId(), saved.getTenantId(), saved.getAccountId(), saved.getDealId()));
        }
        return toResponse(saved, proposalItemRepository.findAllByProposalIdOrderByOrderNo(id));
    }

    @Transactional
    public InvoiceResponse generateInvoiceFromProposal(UUID proposalId) {
        Proposal proposal = findForTenant(proposalId);
        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new BusinessException("Invoice can only be generated from an ACCEPTED proposal");
        }
        if (invoiceRepository.existsByProposalId(proposalId)) {
            throw new BusinessException("An invoice has already been generated for this proposal");
        }
        UUID tenantId = proposal.getTenantId();
        long count = invoiceRepository.countByTenantId(tenantId);
        String invoiceNumber = String.format("INV-%d-%05d", LocalDate.now().getYear(), count + 1);

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setAccountId(proposal.getAccountId());
        invoice.setDealId(proposal.getDealId());
        invoice.setProposalId(proposalId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setTotalAmount(proposal.getTotalValue());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<ProposalItem> proposalItems = proposalItemRepository.findAllByProposalIdOrderByOrderNo(proposalId);
        List<com.risecode.riseflow.commercial.domain.InvoiceItem> invoiceItems = proposalItems.stream()
                .map(pi -> {
                    var ii = new com.risecode.riseflow.commercial.domain.InvoiceItem();
                    ii.setTenantId(tenantId);
                    ii.setInvoiceId(savedInvoice.getId());
                    ii.setDescription(pi.getDescription());
                    ii.setQuantity(pi.getQuantity());
                    ii.setUnitPrice(pi.getUnitPrice());
                    ii.setTotal(pi.getTotal());
                    ii.setOrderNo(pi.getOrderNo());
                    return invoiceItemRepository.save(ii);
                }).toList();

        return toInvoiceResponse(savedInvoice, invoiceItems);
    }

    @Transactional
    public void deleteProposal(UUID id) {
        Proposal proposal = findForTenant(id);
        if (proposal.getStatus() != ProposalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT proposals can be deleted");
        }
        if (invoiceRepository.existsByProposalId(id)) {
            throw new BusinessException("Cannot delete proposal with a generated invoice");
        }
        proposalItemRepository.deleteAllByProposalId(id);
        proposalRepository.delete(proposal);
    }

    // --- Private helpers ---

    private void validateValidUntil(LocalDate validUntil) {
        if (!validUntil.isAfter(LocalDate.now())) {
            throw new BusinessException("validUntil must be a future date");
        }
    }

    private void validateStatusTransition(ProposalStatus current, ProposalStatus next) {
        boolean valid = switch (next) {
            case SENT -> current == ProposalStatus.DRAFT;
            case ACCEPTED, REJECTED, EXPIRED -> current == ProposalStatus.SENT;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("Invalid status transition: " + current + " → " + next);
        }
    }

    private List<ProposalItem> saveItems(List<ProposalItemRequest> requests, UUID proposalId, UUID tenantId) {
        return requests.stream().map(r -> {
            ProposalItem item = new ProposalItem();
            item.setTenantId(tenantId);
            item.setProposalId(proposalId);
            item.setDescription(r.description());
            item.setQuantity(r.quantity());
            item.setUnitPrice(r.unitPrice());
            item.setTotal(r.quantity().multiply(r.unitPrice()));
            item.setOrderNo(r.orderNo());
            return proposalItemRepository.save(item);
        }).toList();
    }

    private BigDecimal computeTotal(List<ProposalItem> items) {
        return items.stream().map(ProposalItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Proposal findForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));
        if (!proposal.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Proposal tenant does not match");
        }
        return proposal;
    }

    ProposalResponse toResponse(Proposal p, List<ProposalItem> items) {
        return new ProposalResponse(p.getId(), p.getTenantId(), p.getAccountId(), p.getDealId(),
                p.getTitle(), p.getDescription(), p.getStatus(), p.getValidUntil(),
                p.getTotalValue(), items.stream().map(this::toItemResponse).toList(),
                p.getCustomFields(), p.getCreatedBy(), p.getCreatedAt());
    }

    ProposalItemResponse toItemResponse(ProposalItem i) {
        return new ProposalItemResponse(i.getId(), i.getProposalId(), i.getDescription(),
                i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getOrderNo());
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv, List<com.risecode.riseflow.commercial.domain.InvoiceItem> items) {
        List<InvoiceItemResponse> itemResponses = items.stream()
                .map(i -> new InvoiceItemResponse(i.getId(), i.getInvoiceId(), i.getDescription(),
                        i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getOrderNo()))
                .toList();
        return new InvoiceResponse(inv.getId(), inv.getTenantId(), inv.getAccountId(), inv.getDealId(),
                inv.getProposalId(), inv.getInvoiceNumber(), inv.getStatus(), inv.getDueDate(),
                inv.getTotalAmount(), inv.getPaidAmount(), inv.getPaymentMethod(),
                inv.getStripeInvoiceId(), inv.getStripePaymentIntentId(), inv.getPaidAt(),
                itemResponses, inv.getCreatedAt());
    }
}
