package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.Commission;
import com.risecode.riseflow.commercial.domain.CommissionStatus;
import com.risecode.riseflow.commercial.events.InvoicePaidEvent;
import com.risecode.riseflow.commercial.persistence.CommissionRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.events.DealWonEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionService.class);
    private static final BigDecimal DEFAULT_PERCENTAGE = BigDecimal.valueOf(10);

    private final CommissionRepository commissionRepository;

    public CommissionService(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDealWon(DealWonEvent event) {
        if (event.responsibleId() == null) {
            log.info("Deal {} won but has no responsibleId — skipping commission", event.dealId());
            return;
        }
        TenantContextHolder.setTenantId(event.tenantId());
        try {
            Commission commission = new Commission();
            commission.setTenantId(event.tenantId());
            commission.setDealId(event.dealId());
            commission.setUserId(event.responsibleId());
            commission.setPercentage(DEFAULT_PERCENTAGE);
            commission.setAmount(event.amount()
                    .multiply(DEFAULT_PERCENTAGE)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            commission.setStatus(CommissionStatus.PENDING);
            commissionRepository.save(commission);
            log.info("Commission created for deal {} user {}", event.dealId(), event.responsibleId());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoicePaid(InvoicePaidEvent event) {
        if (event.dealId() == null) return;
        TenantContextHolder.setTenantId(event.tenantId());
        try {
            List<Commission> pending = commissionRepository
                    .findAllByTenantIdAndDealIdAndStatus(event.tenantId(), event.dealId(), CommissionStatus.PENDING);
            for (Commission c : pending) {
                c.setStatus(CommissionStatus.APPROVED);
                commissionRepository.save(c);
            }
            log.info("Approved {} commissions for deal {} after invoice paid", pending.size(), event.dealId());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Transactional(readOnly = true)
    public List<CommissionResponse> listCommissions(UUID userId, UUID dealId, CommissionStatus status) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return commissionRepository.findFiltered(tenantId, userId, dealId, status)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CommissionResponse getCommission(UUID id) {
        return toResponse(findForTenant(id));
    }

    @Transactional
    public CommissionResponse approveCommission(UUID id) {
        Commission commission = findForTenant(id);
        if (commission.getStatus() != CommissionStatus.PENDING) {
            throw new BusinessException("Only PENDING commissions can be approved");
        }
        commission.setStatus(CommissionStatus.APPROVED);
        return toResponse(commissionRepository.save(commission));
    }

    @Transactional
    public CommissionResponse payCommission(UUID id) {
        Commission commission = findForTenant(id);
        if (commission.getStatus() != CommissionStatus.APPROVED) {
            throw new BusinessException("Only APPROVED commissions can be paid");
        }
        commission.setStatus(CommissionStatus.PAID);
        commission.setPaidAt(Instant.now());
        return toResponse(commissionRepository.save(commission));
    }

    private Commission findForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commission not found: " + id));
        if (!commission.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Commission tenant does not match");
        }
        return commission;
    }

    CommissionResponse toResponse(Commission c) {
        return new CommissionResponse(c.getId(), c.getTenantId(), c.getDealId(), c.getUserId(),
                c.getProposalId(), c.getInvoiceId(), c.getPercentage(), c.getAmount(),
                c.getStatus(), c.getPaidAt(), c.getCreatedAt());
    }
}
