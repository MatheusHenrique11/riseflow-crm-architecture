package com.risecode.riseflow.tenant.api;

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
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_tenant:write')")
    ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_tenant:read')")
    TenantResponse get(@PathVariable UUID id) {
        return tenantService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_tenant:read')")
    List<TenantResponse> list() {
        return tenantService.list();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_tenant:write')")
    TenantResponse update(@PathVariable UUID id, @Valid @RequestBody TenantRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_tenant:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
