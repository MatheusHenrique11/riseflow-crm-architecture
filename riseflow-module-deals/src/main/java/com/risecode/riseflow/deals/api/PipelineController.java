package com.risecode.riseflow.deals.api;

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
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    ResponseEntity<PipelineResponse> create(@Valid @RequestBody PipelineRequest request) {
        PipelineResponse response = pipelineService.createPipeline(request);
        return ResponseEntity.created(URI.create("/api/v1/pipelines/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_pipeline:read')")
    List<PipelineResponse> list() {
        return pipelineService.listPipelines();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:read')")
    PipelineResponse get(@PathVariable UUID id) {
        return pipelineService.getPipeline(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    PipelineResponse update(@PathVariable UUID id, @Valid @RequestBody PipelineRequest request) {
        return pipelineService.updatePipeline(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        pipelineService.deletePipeline(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pipelineId}/stages")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    ResponseEntity<StageResponse> addStage(@PathVariable UUID pipelineId,
            @Valid @RequestBody StageRequest request) {
        StageResponse response = pipelineService.addStage(pipelineId, request);
        return ResponseEntity.created(
                URI.create("/api/v1/pipelines/" + pipelineId + "/stages/" + response.id())).body(response);
    }

    @PutMapping("/{pipelineId}/stages/{stageId}")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    StageResponse updateStage(@PathVariable UUID pipelineId,
            @PathVariable UUID stageId,
            @Valid @RequestBody StageRequest request) {
        return pipelineService.updateStage(pipelineId, stageId, request);
    }

    @DeleteMapping("/{pipelineId}/stages/{stageId}")
    @PreAuthorize("hasAuthority('SCOPE_pipeline:write')")
    ResponseEntity<Void> deleteStage(@PathVariable UUID pipelineId, @PathVariable UUID stageId) {
        pipelineService.deleteStage(pipelineId, stageId);
        return ResponseEntity.noContent().build();
    }
}
