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
@RequestMapping("/api/v1/email-templates")
public class EmailTemplateController {

    private final CampaignService campaignService;

    public EmailTemplateController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<EmailTemplateResponse> createTemplate(@Valid @RequestBody EmailTemplateRequest request) {
        EmailTemplateResponse response = campaignService.createTemplate(request);
        return ResponseEntity.created(URI.create("/api/v1/email-templates/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    List<EmailTemplateResponse> listTemplates() {
        return campaignService.listTemplates();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    EmailTemplateResponse getTemplate(@PathVariable UUID id) {
        return campaignService.getTemplate(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    EmailTemplateResponse updateTemplate(@PathVariable UUID id, @Valid @RequestBody EmailTemplateRequest request) {
        return campaignService.updateTemplate(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        campaignService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
