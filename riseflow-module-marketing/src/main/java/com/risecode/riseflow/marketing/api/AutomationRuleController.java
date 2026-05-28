package com.risecode.riseflow.marketing.api;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/automation-rules")
public class AutomationRuleController {

    private final AutomationService automationService;

    public AutomationRuleController(AutomationService automationService) {
        this.automationService = automationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<AutomationRuleResponse> createRule(@Valid @RequestBody AutomationRuleRequest request) {
        AutomationRuleResponse response = automationService.createRule(request);
        return ResponseEntity.created(URI.create("/api/v1/automation-rules/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    List<AutomationRuleResponse> listRules() {
        return automationService.listRules();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    AutomationRuleResponse getRule(@PathVariable UUID id) {
        return automationService.getRule(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    AutomationRuleResponse updateRule(@PathVariable UUID id, @Valid @RequestBody AutomationRuleRequest request) {
        return automationService.updateRule(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        automationService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
