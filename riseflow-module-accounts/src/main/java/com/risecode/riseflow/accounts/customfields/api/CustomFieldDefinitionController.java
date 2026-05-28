package com.risecode.riseflow.accounts.customfields.api;

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
@RequestMapping("/api/v1/custom-field-definitions")
public class CustomFieldDefinitionController {

    private final CustomFieldDefinitionService service;

    public CustomFieldDefinitionController(CustomFieldDefinitionService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_custom-field:write')")
    ResponseEntity<CustomFieldDefinitionResponse> create(@Valid @RequestBody CustomFieldDefinitionRequest request) {
        CustomFieldDefinitionResponse response = service.createDefinition(request);
        return ResponseEntity
                .created(URI.create("/api/v1/custom-field-definitions/" + response.id()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_custom-field:read')")
    List<CustomFieldDefinitionResponse> list(@RequestParam String entityType) {
        return service.getDefinitionsForTenant(entityType);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_custom-field:read')")
    CustomFieldDefinitionResponse get(@PathVariable UUID id) {
        return service.getDefinition(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_custom-field:write')")
    CustomFieldDefinitionResponse update(@PathVariable UUID id, @Valid @RequestBody CustomFieldDefinitionRequest request) {
        return service.updateDefinition(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_custom-field:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }
}
