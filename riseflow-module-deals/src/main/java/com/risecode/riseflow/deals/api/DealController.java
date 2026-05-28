package com.risecode.riseflow.deals.api;

import com.risecode.riseflow.deals.domain.DealStatus;
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
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_deal:write')")
    ResponseEntity<DealResponse> create(@Valid @RequestBody DealRequest request) {
        DealResponse response = dealService.createDeal(request);
        return ResponseEntity.created(URI.create("/api/v1/deals/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_deal:read')")
    DealResponse get(@PathVariable UUID id) {
        return dealService.getDeal(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_deal:read')")
    List<DealResponse> list(
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) UUID responsibleId,
            @RequestParam(required = false) DealStatus status) {
        return dealService.listDeals(pipelineId, stageId, responsibleId, status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_deal:write')")
    DealResponse update(@PathVariable UUID id, @Valid @RequestBody DealRequest request) {
        return dealService.updateDeal(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_deal:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        dealService.deleteDeal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('SCOPE_deal:write')")
    DealResponse move(@PathVariable UUID id, @Valid @RequestBody MoveStageRequest request) {
        return dealService.moveDeal(id, request);
    }
}
