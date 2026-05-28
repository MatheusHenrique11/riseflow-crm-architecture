package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.ProposalStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_proposal:write')")
    ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalRequest request) {
        ProposalResponse response = proposalService.createProposal(request);
        return ResponseEntity.created(URI.create("/api/v1/proposals/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_proposal:read')")
    List<ProposalResponse> list(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID dealId,
            @RequestParam(required = false) ProposalStatus status) {
        return proposalService.listProposals(accountId, dealId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_proposal:read')")
    ProposalResponse get(@PathVariable UUID id) {
        return proposalService.getProposal(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_proposal:write')")
    ProposalResponse update(@PathVariable UUID id, @Valid @RequestBody ProposalRequest request) {
        return proposalService.updateProposal(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_proposal:write')")
    ProposalResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ProposalStatusRequest request) {
        return proposalService.changeStatus(id, request);
    }

    @PostMapping("/{id}/generate-invoice")
    @PreAuthorize("hasAuthority('SCOPE_invoice:write')")
    ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable UUID id) {
        InvoiceResponse response = proposalService.generateInvoiceFromProposal(id);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + response.id())).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_proposal:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.noContent().build();
    }
}
