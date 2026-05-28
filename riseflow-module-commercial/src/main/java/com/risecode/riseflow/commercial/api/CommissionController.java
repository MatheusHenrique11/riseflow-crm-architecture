package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.CommissionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commissions")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_commission:read')")
    List<CommissionResponse> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID dealId,
            @RequestParam(required = false) CommissionStatus status) {
        return commissionService.listCommissions(userId, dealId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_commission:read')")
    CommissionResponse get(@PathVariable UUID id) {
        return commissionService.getCommission(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCOPE_commission:write')")
    CommissionResponse approve(@PathVariable UUID id) {
        return commissionService.approveCommission(id);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('SCOPE_commission:write')")
    CommissionResponse pay(@PathVariable UUID id) {
        return commissionService.payCommission(id);
    }
}
