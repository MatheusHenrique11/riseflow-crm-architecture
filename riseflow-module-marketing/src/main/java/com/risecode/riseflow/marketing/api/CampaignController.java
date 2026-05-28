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
@RequestMapping("/api/v1")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.created(URI.create("/api/v1/campaigns/" + response.id())).body(response);
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    List<CampaignResponse> listCampaigns() {
        return campaignService.listCampaigns();
    }

    @GetMapping("/campaigns/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    CampaignResponse getCampaign(@PathVariable UUID id) {
        return campaignService.getCampaign(id);
    }

    @PutMapping("/campaigns/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    CampaignResponse updateCampaign(@PathVariable UUID id, @Valid @RequestBody CampaignRequest request) {
        return campaignService.updateCampaign(id, request);
    }

    @PutMapping("/campaigns/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    CampaignResponse changeCampaignStatus(@PathVariable UUID id, @RequestBody CampaignStatusRequest request) {
        return campaignService.changeStatus(id, request);
    }

    @DeleteMapping("/campaigns/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<Void> deleteCampaign(@PathVariable UUID id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/campaigns/{campaignId}/sequences")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<SequenceResponse> addSequence(@PathVariable UUID campaignId, @Valid @RequestBody SequenceRequest request) {
        SequenceResponse response = campaignService.addSequence(campaignId, request);
        return ResponseEntity.created(URI.create("/api/v1/sequences/" + response.id())).body(response);
    }

    @GetMapping("/sequences/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    SequenceResponse getSequence(@PathVariable UUID id) {
        return campaignService.getSequence(id);
    }

    @PutMapping("/sequences/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    SequenceResponse updateSequence(@PathVariable UUID id, @Valid @RequestBody SequenceRequest request) {
        return campaignService.updateSequence(id, request);
    }

    @DeleteMapping("/sequences/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<Void> deleteSequence(@PathVariable UUID id) {
        campaignService.deleteSequence(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sequences/{sequenceId}/steps")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<SequenceStepResponse> addStep(@PathVariable UUID sequenceId, @Valid @RequestBody SequenceStepRequest request) {
        SequenceStepResponse response = campaignService.addStep(sequenceId, request);
        return ResponseEntity.created(URI.create("/api/v1/steps/" + response.id())).body(response);
    }

    @PutMapping("/steps/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    SequenceStepResponse updateStep(@PathVariable UUID id, @Valid @RequestBody SequenceStepRequest request) {
        return campaignService.updateStep(id, request);
    }

    @DeleteMapping("/steps/{id}")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    ResponseEntity<Void> deleteStep(@PathVariable UUID id) {
        campaignService.deleteStep(id);
        return ResponseEntity.noContent().build();
    }
}
