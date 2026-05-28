package com.risecode.riseflow.commercial.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock ProposalRepository proposalRepository;
    @Mock ProposalItemRepository proposalItemRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceItemRepository invoiceItemRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks ProposalService proposalService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateProposal_andCalculateTotalFromItems() {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
            Proposal p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(proposalItemRepository.save(any(ProposalItem.class))).thenAnswer(inv -> {
            ProposalItem i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        ProposalRequest request = new ProposalRequest(
                accountId, null, "My Proposal", "Description",
                LocalDate.now().plusDays(10),
                List.of(
                        new ProposalItemRequest("Item A", BigDecimal.valueOf(2), BigDecimal.valueOf(100), 1),
                        new ProposalItemRequest("Item B", BigDecimal.valueOf(3), BigDecimal.valueOf(50), 2)),
                null);

        ProposalResponse response = proposalService.createProposal(request);

        assertThat(response.totalValue()).isEqualByComparingTo(BigDecimal.valueOf(350));
        assertThat(response.status()).isEqualTo(ProposalStatus.DRAFT);
    }

    @Test
    void shouldRejectCreate_whenValidUntilIsNotFuture() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        ProposalRequest request = new ProposalRequest(
                UUID.randomUUID(), null, "Title", null,
                LocalDate.now(), // today is not in the future
                List.of(new ProposalItemRequest("Item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null);

        assertThatThrownBy(() -> proposalService.createProposal(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("future");
    }

    @Test
    void shouldChangeStatus_fromDraftToSent() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.DRAFT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(proposalItemRepository.findAllByProposalIdOrderByOrderNo(proposalId)).thenReturn(List.of());

        ProposalResponse response = proposalService.changeStatus(proposalId, new ProposalStatusRequest(ProposalStatus.SENT));

        assertThat(response.status()).isEqualTo(ProposalStatus.SENT);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldFireProposalAcceptedEvent_whenStatusChangedToAccepted() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.SENT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(proposalItemRepository.findAllByProposalIdOrderByOrderNo(proposalId)).thenReturn(List.of());

        proposalService.changeStatus(proposalId, new ProposalStatusRequest(ProposalStatus.ACCEPTED));

        ArgumentCaptor<ProposalAcceptedEvent> captor = ArgumentCaptor.forClass(ProposalAcceptedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().proposalId()).isEqualTo(proposalId);
    }

    @Test
    void shouldRejectStatusTransition_whenInvalid() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.DRAFT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.changeStatus(proposalId, new ProposalStatusRequest(ProposalStatus.ACCEPTED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldRejectUpdate_whenProposalIsNotDraft() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.SENT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        ProposalRequest request = new ProposalRequest(
                UUID.randomUUID(), null, "Title", null,
                LocalDate.now().plusDays(5),
                List.of(new ProposalItemRequest("X", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null);

        assertThatThrownBy(() -> proposalService.updateProposal(proposalId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldGenerateInvoiceFromAcceptedProposal() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.ACCEPTED);
        proposal.setTotalValue(BigDecimal.valueOf(500));
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(invoiceRepository.existsByProposalId(proposalId)).thenReturn(false);
        when(invoiceRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });
        when(proposalItemRepository.findAllByProposalIdOrderByOrderNo(proposalId)).thenReturn(List.of());

        InvoiceResponse response = proposalService.generateInvoiceFromProposal(proposalId);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void shouldRejectGenerateInvoice_whenProposalNotAccepted() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.SENT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.generateInvoiceFromProposal(proposalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void shouldRejectGenerateInvoice_whenInvoiceAlreadyExists() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.ACCEPTED);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(invoiceRepository.existsByProposalId(proposalId)).thenReturn(true);

        assertThatThrownBy(() -> proposalService.generateInvoiceFromProposal(proposalId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been generated");
    }

    @Test
    void shouldDeleteProposal_whenDraftAndNoInvoice() {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, tenantId, ProposalStatus.DRAFT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));
        when(invoiceRepository.existsByProposalId(proposalId)).thenReturn(false);

        proposalService.deleteProposal(proposalId);

        verify(proposalRepository).delete(proposal);
    }

    @Test
    void shouldThrowTenantMismatch_whenProposalBelongsToDifferentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Proposal proposal = proposal(proposalId, otherTenantId, ProposalStatus.DRAFT);
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.getProposal(proposalId))
                .isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void shouldThrowEntityNotFound_whenProposalDoesNotExist() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        UUID proposalId = UUID.randomUUID();
        when(proposalRepository.findById(proposalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getProposal(proposalId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Proposal proposal(UUID id, UUID tenantId, ProposalStatus status) {
        Proposal p = new Proposal();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setAccountId(UUID.randomUUID());
        p.setTitle("Proposal");
        p.setValidUntil(LocalDate.now().plusDays(30));
        p.setStatus(status);
        p.setTotalValue(BigDecimal.ZERO);
        p.setCustomFields(new java.util.HashMap<>());
        return p;
    }
}
