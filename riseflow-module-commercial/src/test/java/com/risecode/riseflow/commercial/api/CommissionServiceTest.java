package com.risecode.riseflow.commercial.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock CommissionRepository commissionRepository;

    @InjectMocks CommissionService commissionService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateCommission_whenDealWon() {
        UUID dealId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID responsibleId = UUID.randomUUID();

        when(commissionRepository.save(any(Commission.class))).thenAnswer(inv -> {
            Commission c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        DealWonEvent event = new DealWonEvent(dealId, tenantId, UUID.randomUUID(), responsibleId,
                BigDecimal.valueOf(10000), "USD");

        commissionService.onDealWon(event);

        ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(captor.capture());
        Commission saved = captor.getValue();
        assertThat(saved.getDealId()).isEqualTo(dealId);
        assertThat(saved.getUserId()).isEqualTo(responsibleId);
        assertThat(saved.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000)); // 10% of 10000
        assertThat(saved.getStatus()).isEqualTo(CommissionStatus.PENDING);
    }

    @Test
    void shouldSkipCommission_whenResponsibleIdIsNull() {
        DealWonEvent event = new DealWonEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                BigDecimal.valueOf(5000), "USD");

        commissionService.onDealWon(event);

        verify(commissionRepository, never()).save(any());
    }

    @Test
    void shouldApproveCommissions_whenInvoicePaid() {
        UUID tenantId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Commission c1 = commission(UUID.randomUUID(), tenantId, dealId, CommissionStatus.PENDING);
        Commission c2 = commission(UUID.randomUUID(), tenantId, dealId, CommissionStatus.PENDING);
        when(commissionRepository.findAllByTenantIdAndDealIdAndStatus(tenantId, dealId, CommissionStatus.PENDING))
                .thenReturn(List.of(c1, c2));
        when(commissionRepository.save(any(Commission.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoicePaidEvent event = new InvoicePaidEvent(invoiceId, tenantId, dealId, BigDecimal.valueOf(5000));

        commissionService.onInvoicePaid(event);

        assertThat(c1.getStatus()).isEqualTo(CommissionStatus.APPROVED);
        assertThat(c2.getStatus()).isEqualTo(CommissionStatus.APPROVED);
    }

    @Test
    void shouldSkipInvoicePaid_whenNoDealId() {
        InvoicePaidEvent event = new InvoicePaidEvent(UUID.randomUUID(), UUID.randomUUID(), null, BigDecimal.TEN);

        commissionService.onInvoicePaid(event);

        verify(commissionRepository, never()).findAllByTenantIdAndDealIdAndStatus(any(), any(), any());
    }

    @Test
    void shouldApproveCommission_whenPending() {
        UUID tenantId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Commission c = commission(commissionId, tenantId, UUID.randomUUID(), CommissionStatus.PENDING);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(c));
        when(commissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommissionResponse response = commissionService.approveCommission(commissionId);

        assertThat(response.status()).isEqualTo(CommissionStatus.APPROVED);
    }

    @Test
    void shouldRejectApprove_whenNotPending() {
        UUID tenantId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Commission c = commission(commissionId, tenantId, UUID.randomUUID(), CommissionStatus.APPROVED);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commissionService.approveCommission(commissionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void shouldPayCommission_whenApproved() {
        UUID tenantId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Commission c = commission(commissionId, tenantId, UUID.randomUUID(), CommissionStatus.APPROVED);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(c));
        when(commissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CommissionResponse response = commissionService.payCommission(commissionId);

        assertThat(response.status()).isEqualTo(CommissionStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    void shouldRejectPay_whenNotApproved() {
        UUID tenantId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Commission c = commission(commissionId, tenantId, UUID.randomUUID(), CommissionStatus.PENDING);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commissionService.payCommission(commissionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void shouldThrowTenantMismatch_whenCommissionBelongsToDifferentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID commissionId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Commission c = commission(commissionId, otherTenantId, UUID.randomUUID(), CommissionStatus.PENDING);
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commissionService.getCommission(commissionId))
                .isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void shouldThrowEntityNotFound_whenCommissionDoesNotExist() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        UUID commissionId = UUID.randomUUID();
        when(commissionRepository.findById(commissionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commissionService.getCommission(commissionId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Commission commission(UUID id, UUID tenantId, UUID dealId, CommissionStatus status) {
        Commission c = new Commission();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setDealId(dealId);
        c.setUserId(UUID.randomUUID());
        c.setPercentage(BigDecimal.valueOf(10));
        c.setAmount(BigDecimal.valueOf(1000));
        c.setStatus(status);
        return c;
    }
}
