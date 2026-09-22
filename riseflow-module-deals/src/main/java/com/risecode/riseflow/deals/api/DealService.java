package com.risecode.riseflow.deals.api;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.core.dto.PageResponse;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.domain.Deal;
import com.risecode.riseflow.deals.domain.DealStatus;
import com.risecode.riseflow.deals.domain.Pipeline;
import com.risecode.riseflow.deals.domain.Stage;
import com.risecode.riseflow.deals.events.DealStageChangedEvent;
import com.risecode.riseflow.deals.events.DealWonEvent;
import com.risecode.riseflow.deals.persistence.DealRepository;
import com.risecode.riseflow.deals.persistence.PipelineRepository;
import com.risecode.riseflow.deals.persistence.StageRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealService {

    private final DealRepository dealRepository;
    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DealService(DealRepository dealRepository,
            PipelineRepository pipelineRepository,
            StageRepository stageRepository,
            CustomFieldDefinitionRepository customFieldDefinitionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.dealRepository = dealRepository;
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DealResponse createDeal(DealRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Pipeline pipeline = findPipelineForTenant(request.pipelineId(), tenantId);
        Stage stage = findStage(request.stageId());
        if (!stage.getPipelineId().equals(pipeline.getId())) {
            throw new BusinessException("Stage does not belong to the specified pipeline");
        }
        Map<String, Object> customFields = request.customFields() != null ? request.customFields() : Map.of();
        validateDealCustomFields(tenantId, customFields);

        Deal deal = new Deal();
        deal.setTenantId(tenantId);
        applyRequest(deal, request, stage);
        return toResponse(dealRepository.save(deal));
    }

    @Transactional(readOnly = true)
    public DealResponse getDeal(UUID id) {
        return toResponse(findForCurrentTenant(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DealResponse> listDeals(
            UUID pipelineId, UUID stageId, UUID responsibleId, DealStatus status, int page, int size) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Specification<Deal> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (pipelineId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pipelineId"), pipelineId));
        }
        if (stageId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("stageId"), stageId));
        }
        if (responsibleId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("responsibleId"), responsibleId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<Deal> result = dealRepository.findAll(spec, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public DealResponse updateDeal(UUID id, DealRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Deal deal = findForCurrentTenant(id);
        Pipeline pipeline = findPipelineForTenant(request.pipelineId(), tenantId);
        Stage stage = findStage(request.stageId());
        if (!stage.getPipelineId().equals(pipeline.getId())) {
            throw new BusinessException("Stage does not belong to the specified pipeline");
        }
        Map<String, Object> customFields = request.customFields() != null ? request.customFields() : Map.of();
        validateDealCustomFields(tenantId, customFields);
        applyRequest(deal, request, stage);
        return toResponse(dealRepository.save(deal));
    }

    @Transactional
    public void deleteDeal(UUID id) {
        dealRepository.delete(findForCurrentTenant(id));
    }

    @Transactional
    public DealResponse moveDeal(UUID id, MoveStageRequest request) {
        Deal deal = findForCurrentTenant(id);
        UUID previousStageId = deal.getStageId();
        Stage targetStage = findStage(request.stageId());
        if (!targetStage.getPipelineId().equals(deal.getPipelineId())) {
            throw new BusinessException("Target stage is not in the same pipeline as the deal");
        }
        deal.setStageId(targetStage.getId());
        deal.setProbability(targetStage.getProbability());
        deal.setStatus(DealStatus.fromProbability(targetStage.getProbability()));
        Deal saved = dealRepository.save(deal);
        eventPublisher.publishEvent(new DealStageChangedEvent(
                saved.getId(), saved.getTenantId(), previousStageId,
                targetStage.getId(), saved.getAccountId(), null));
        if (saved.getStatus() == DealStatus.WON) {
            eventPublisher.publishEvent(new DealWonEvent(
                    saved.getId(), saved.getTenantId(), saved.getAccountId(),
                    saved.getResponsibleId(), saved.getAmount(), saved.getCurrency()));
        }
        return toResponse(saved);
    }

    void validateDealCustomFields(UUID tenantId, Map<String, Object> customFields) {
        List<CustomFieldDefinition> definitions =
                customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL");
        for (CustomFieldDefinition def : definitions) {
            Object value = customFields.get(def.getFieldName());
            if (def.isRequired() && value == null) {
                throw new BusinessException("Required custom field missing: " + def.getFieldName());
            }
            if (value != null) {
                validateFieldValue(def, value);
            }
        }
    }

    private void validateFieldValue(CustomFieldDefinition def, Object value) {
        switch (def.getFieldType()) {
            case NUMBER -> {
                if (!(value instanceof Number)) {
                    try {
                        Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        throw new BusinessException(
                                "Custom field '" + def.getFieldName() + "' must be a number, got: " + value);
                    }
                }
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    String str = value.toString().toLowerCase();
                    if (!str.equals("true") && !str.equals("false")) {
                        throw new BusinessException(
                                "Custom field '" + def.getFieldName() + "' must be a boolean, got: " + value);
                    }
                }
            }
            case PICKLIST -> {
                String strValue = value.toString();
                if (def.getPicklistOptions() == null || !def.getPicklistOptions().contains(strValue)) {
                    throw new BusinessException(
                            "Custom field '" + def.getFieldName() + "' must be one of: "
                                    + def.getPicklistOptions() + ", got: " + strValue);
                }
            }
            default -> {
            }
        }
    }

    private void applyRequest(Deal deal, DealRequest request, Stage stage) {
        deal.setTitle(request.title());
        deal.setPipelineId(request.pipelineId());
        deal.setStageId(request.stageId());
        deal.setAccountId(request.accountId());
        deal.setAmount(request.amount() != null ? request.amount() : BigDecimal.ZERO);
        deal.setCurrency(request.currency() != null ? request.currency() : "USD");
        deal.setExpectedCloseDate(request.expectedCloseDate());
        deal.setResponsibleId(request.responsibleId());
        deal.setNotes(request.notes());
        deal.setCustomFields(request.customFields() != null ? new HashMap<>(request.customFields()) : new HashMap<>());
        deal.setProbability(stage.getProbability());
        deal.setStatus(DealStatus.fromProbability(stage.getProbability()));
    }

    private Deal findForCurrentTenant(UUID id) {
        UUID currentTenantId = TenantContextHolder.requireTenantId();
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deal not found: " + id));
        if (!currentTenantId.equals(deal.getTenantId())) {
            throw new TenantMismatchException("Deal tenant does not match active tenant");
        }
        return deal;
    }

    private Pipeline findPipelineForTenant(UUID pipelineId, UUID tenantId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new EntityNotFoundException("Pipeline not found: " + pipelineId));
        if (!tenantId.equals(pipeline.getTenantId())) {
            throw new TenantMismatchException("Pipeline tenant does not match active tenant");
        }
        return pipeline;
    }

    private Stage findStage(UUID stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Stage not found: " + stageId));
    }

    private DealResponse toResponse(Deal deal) {
        return new DealResponse(
                deal.getId(),
                deal.getTenantId(),
                deal.getPipelineId(),
                deal.getStageId(),
                deal.getAccountId(),
                deal.getTitle(),
                deal.getAmount(),
                deal.getCurrency(),
                deal.getProbability(),
                deal.getStatus(),
                deal.getExpectedCloseDate(),
                deal.getActualCloseDate(),
                deal.getResponsibleId(),
                deal.getNotes(),
                deal.getCustomFields());
    }
}
