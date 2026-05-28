package com.risecode.riseflow.deals.api;

import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.domain.Pipeline;
import com.risecode.riseflow.deals.domain.Stage;
import com.risecode.riseflow.deals.persistence.DealRepository;
import com.risecode.riseflow.deals.persistence.PipelineRepository;
import com.risecode.riseflow.deals.persistence.StageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final DealRepository dealRepository;

    public PipelineService(PipelineRepository pipelineRepository,
            StageRepository stageRepository,
            DealRepository dealRepository) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.dealRepository = dealRepository;
    }

    @Transactional
    public PipelineResponse createPipeline(PipelineRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        if (request.defaultPipeline()) {
            clearExistingDefaults(tenantId);
        }
        Pipeline pipeline = new Pipeline();
        pipeline.setTenantId(tenantId);
        pipeline.setName(request.name());
        pipeline.setDescription(request.description());
        pipeline.setDefaultPipeline(request.defaultPipeline());
        Pipeline saved = pipelineRepository.save(pipeline);

        List<Stage> stages = List.of();
        if (request.stages() != null && !request.stages().isEmpty()) {
            stages = request.stages().stream()
                    .map(sr -> createStageEntity(saved.getId(), tenantId, sr))
                    .map(stageRepository::save)
                    .toList();
        }
        return toResponse(saved, stages);
    }

    @Transactional(readOnly = true)
    public PipelineResponse getPipeline(UUID id) {
        Pipeline pipeline = findForCurrentTenant(id);
        List<Stage> stages = stageRepository.findAllByPipelineIdOrderByPosition(id);
        return toResponse(pipeline, stages);
    }

    @Transactional(readOnly = true)
    public List<PipelineResponse> listPipelines() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return pipelineRepository.findAllByTenantId(tenantId).stream()
                .map(p -> toResponse(p, stageRepository.findAllByPipelineIdOrderByPosition(p.getId())))
                .toList();
    }

    @Transactional
    public PipelineResponse updatePipeline(UUID id, PipelineRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Pipeline pipeline = findForCurrentTenant(id);
        if (request.defaultPipeline() && !pipeline.isDefaultPipeline()) {
            clearExistingDefaults(tenantId);
        }
        pipeline.setName(request.name());
        pipeline.setDescription(request.description());
        pipeline.setDefaultPipeline(request.defaultPipeline());
        Pipeline saved = pipelineRepository.save(pipeline);
        List<Stage> stages = stageRepository.findAllByPipelineIdOrderByPosition(id);
        return toResponse(saved, stages);
    }

    @Transactional
    public void deletePipeline(UUID id) {
        Pipeline pipeline = findForCurrentTenant(id);
        if (dealRepository.existsByPipelineId(id)) {
            throw new BusinessException("Cannot delete pipeline that has active deals");
        }
        pipelineRepository.delete(pipeline);
    }

    @Transactional
    public StageResponse addStage(UUID pipelineId, StageRequest request) {
        Pipeline pipeline = findForCurrentTenant(pipelineId);
        UUID tenantId = TenantContextHolder.requireTenantId();
        if (stageRepository.existsByPipelineIdAndPosition(pipelineId, request.position())) {
            throw new BusinessException("Stage with position " + request.position() + " already exists in this pipeline");
        }
        Stage stage = createStageEntity(pipelineId, tenantId, request);
        return toStageResponse(stageRepository.save(stage));
    }

    @Transactional
    public StageResponse updateStage(UUID pipelineId, UUID stageId, StageRequest request) {
        findForCurrentTenant(pipelineId);
        Stage stage = findStage(stageId);
        if (!stage.getPipelineId().equals(pipelineId)) {
            throw new BusinessException("Stage does not belong to this pipeline");
        }
        if (stageRepository.existsByPipelineIdAndPositionAndIdNot(pipelineId, request.position(), stageId)) {
            throw new BusinessException("Stage with position " + request.position() + " already exists in this pipeline");
        }
        stage.setName(request.name());
        stage.setPosition(request.position());
        stage.setProbability(request.probability());
        stage.setColor(request.color());
        return toStageResponse(stageRepository.save(stage));
    }

    @Transactional
    public void deleteStage(UUID pipelineId, UUID stageId) {
        findForCurrentTenant(pipelineId);
        Stage stage = findStage(stageId);
        if (!stage.getPipelineId().equals(pipelineId)) {
            throw new BusinessException("Stage does not belong to this pipeline");
        }
        if (dealRepository.existsByStageId(stageId)) {
            throw new BusinessException("Cannot delete stage that has active deals");
        }
        stageRepository.delete(stage);
    }

    private void clearExistingDefaults(UUID tenantId) {
        List<Pipeline> defaults = pipelineRepository.findAllByTenantId(tenantId).stream()
                .filter(Pipeline::isDefaultPipeline)
                .toList();
        defaults.forEach(p -> {
            p.setDefaultPipeline(false);
            pipelineRepository.save(p);
        });
    }

    private Pipeline findForCurrentTenant(UUID id) {
        UUID currentTenantId = TenantContextHolder.requireTenantId();
        Pipeline pipeline = pipelineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pipeline not found: " + id));
        if (!currentTenantId.equals(pipeline.getTenantId())) {
            throw new TenantMismatchException("Pipeline tenant does not match active tenant");
        }
        return pipeline;
    }

    private Stage findStage(UUID stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage not found: " + stageId));
    }

    private Stage createStageEntity(UUID pipelineId, UUID tenantId, StageRequest request) {
        Stage stage = new Stage();
        stage.setTenantId(tenantId);
        stage.setPipelineId(pipelineId);
        stage.setName(request.name());
        stage.setPosition(request.position());
        stage.setProbability(request.probability());
        stage.setColor(request.color());
        return stage;
    }

    private PipelineResponse toResponse(Pipeline pipeline, List<Stage> stages) {
        return new PipelineResponse(
                pipeline.getId(),
                pipeline.getTenantId(),
                pipeline.getName(),
                pipeline.getDescription(),
                pipeline.isDefaultPipeline(),
                stages.stream().map(this::toStageResponse).toList());
    }

    StageResponse toStageResponse(Stage stage) {
        return new StageResponse(
                stage.getId(),
                stage.getTenantId(),
                stage.getPipelineId(),
                stage.getName(),
                stage.getPosition(),
                stage.getProbability(),
                stage.getColor());
    }
}
