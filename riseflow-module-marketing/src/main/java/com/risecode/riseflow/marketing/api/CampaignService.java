package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.marketing.domain.ActionType;
import com.risecode.riseflow.marketing.domain.Campaign;
import com.risecode.riseflow.marketing.domain.CampaignStatus;
import com.risecode.riseflow.marketing.domain.EmailTemplate;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.Sequence;
import com.risecode.riseflow.marketing.domain.SequenceStep;
import com.risecode.riseflow.marketing.persistence.CampaignRepository;
import com.risecode.riseflow.marketing.persistence.EmailTemplateRepository;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import com.risecode.riseflow.marketing.persistence.SequenceRepository;
import com.risecode.riseflow.marketing.persistence.SequenceStepRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final SequenceRepository sequenceRepository;
    private final SequenceStepRepository stepRepository;
    private final EmailTemplateRepository templateRepository;
    private final SequenceEnrollmentRepository enrollmentRepository;

    public CampaignService(CampaignRepository campaignRepository,
            SequenceRepository sequenceRepository,
            SequenceStepRepository stepRepository,
            EmailTemplateRepository templateRepository,
            SequenceEnrollmentRepository enrollmentRepository) {
        this.campaignRepository = campaignRepository;
        this.sequenceRepository = sequenceRepository;
        this.stepRepository = stepRepository;
        this.templateRepository = templateRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Campaign campaign = new Campaign();
        campaign.setTenantId(tenantId);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setStatus(CampaignStatus.DRAFT);
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return campaignRepository.findAllByTenantId(tenantId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID id) {
        return toResponse(findCampaignForTenant(id));
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID id, CampaignRequest request) {
        Campaign campaign = findCampaignForTenant(id);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignResponse changeStatus(UUID id, CampaignStatusRequest request) {
        Campaign campaign = findCampaignForTenant(id);
        CampaignStatus next = request.status();
        validateStatusTransition(campaign, next);
        campaign.setStatus(next);
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public void deleteCampaign(UUID id) {
        Campaign campaign = findCampaignForTenant(id);
        List<Sequence> sequences = sequenceRepository.findAllByCampaignId(id);
        for (Sequence seq : sequences) {
            if (enrollmentRepository.existsBySequenceIdAndStatus(seq.getId(), EnrollmentStatus.ACTIVE)) {
                throw new BusinessException("Cannot delete campaign with active enrollments");
            }
        }
        campaignRepository.delete(campaign);
    }

    @Transactional
    public SequenceResponse addSequence(UUID campaignId, SequenceRequest request) {
        Campaign campaign = findCampaignForTenant(campaignId);
        UUID tenantId = TenantContextHolder.requireTenantId();
        Sequence sequence = new Sequence();
        sequence.setTenantId(tenantId);
        sequence.setCampaignId(campaign.getId());
        sequence.setName(request.name());
        return toSequenceResponse(sequenceRepository.save(sequence));
    }

    @Transactional(readOnly = true)
    public SequenceResponse getSequence(UUID sequenceId) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Sequence sequence = sequenceRepository.findById(sequenceId)
                .orElseThrow(() -> new EntityNotFoundException("Sequence not found: " + sequenceId));
        if (!sequence.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Sequence tenant does not match");
        }
        return toSequenceResponse(sequence);
    }

    @Transactional
    public SequenceResponse updateSequence(UUID sequenceId, SequenceRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Sequence sequence = sequenceRepository.findById(sequenceId)
                .orElseThrow(() -> new EntityNotFoundException("Sequence not found: " + sequenceId));
        if (!sequence.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Sequence tenant does not match");
        }
        sequence.setName(request.name());
        return toSequenceResponse(sequenceRepository.save(sequence));
    }

    @Transactional
    public void deleteSequence(UUID sequenceId) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Sequence sequence = sequenceRepository.findById(sequenceId)
                .orElseThrow(() -> new EntityNotFoundException("Sequence not found: " + sequenceId));
        if (!sequence.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Sequence tenant does not match");
        }
        if (enrollmentRepository.existsBySequenceIdAndStatus(sequenceId, EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Cannot delete sequence with active enrollments");
        }
        sequenceRepository.delete(sequence);
    }

    @Transactional
    public SequenceStepResponse addStep(UUID sequenceId, SequenceStepRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Sequence sequence = sequenceRepository.findById(sequenceId)
                .orElseThrow(() -> new EntityNotFoundException("Sequence not found: " + sequenceId));
        if (!sequence.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Sequence tenant does not match");
        }
        if (stepRepository.existsBySequenceIdAndStepOrder(sequenceId, request.stepOrder())) {
            throw new BusinessException("Step order " + request.stepOrder() + " already exists in this sequence");
        }
        validateStepContent(request);
        SequenceStep step = toStepEntity(new SequenceStep(), request, sequenceId, tenantId);
        return toStepResponse(stepRepository.save(step));
    }

    @Transactional
    public SequenceStepResponse updateStep(UUID stepId, SequenceStepRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        SequenceStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new EntityNotFoundException("Step not found: " + stepId));
        if (!step.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Step tenant does not match");
        }
        if (stepRepository.existsBySequenceIdAndStepOrderAndIdNot(step.getSequenceId(), request.stepOrder(), stepId)) {
            throw new BusinessException("Step order " + request.stepOrder() + " already exists in this sequence");
        }
        validateStepContent(request);
        return toStepResponse(stepRepository.save(toStepEntity(step, request, step.getSequenceId(), tenantId)));
    }

    @Transactional
    public void deleteStep(UUID stepId) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        SequenceStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new EntityNotFoundException("Step not found: " + stepId));
        if (!step.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Step tenant does not match");
        }
        stepRepository.delete(step);
    }

    // --- Email template CRUD ---

    @Transactional
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        EmailTemplate t = new EmailTemplate();
        t.setTenantId(tenantId);
        t.setName(request.name());
        t.setSubject(request.subject());
        t.setBody(request.body());
        return toTemplateResponse(templateRepository.save(t));
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> listTemplates() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return templateRepository.findAllByTenantId(tenantId).stream().map(this::toTemplateResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(UUID id) {
        return toTemplateResponse(findTemplateForTenant(id));
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(UUID id, EmailTemplateRequest request) {
        EmailTemplate t = findTemplateForTenant(id);
        t.setName(request.name());
        t.setSubject(request.subject());
        t.setBody(request.body());
        return toTemplateResponse(templateRepository.save(t));
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        templateRepository.delete(findTemplateForTenant(id));
    }

    // --- Private helpers ---

    private void validateStatusTransition(Campaign campaign, CampaignStatus next) {
        CampaignStatus current = campaign.getStatus();
        if (current == CampaignStatus.COMPLETED) {
            throw new BusinessException("Cannot change status of a completed campaign");
        }
        if (next == CampaignStatus.ACTIVE) {
            List<Sequence> sequences = sequenceRepository.findAllByCampaignId(campaign.getId());
            if (sequences.isEmpty()) {
                throw new BusinessException("Cannot activate campaign without sequences");
            }
            for (Sequence seq : sequences) {
                List<SequenceStep> steps = stepRepository.findAllBySequenceIdOrderByStepOrder(seq.getId());
                if (steps.isEmpty()) {
                    throw new BusinessException("Sequence '" + seq.getName() + "' has no steps");
                }
            }
        }
    }

    private void validateStepContent(SequenceStepRequest request) {
        if (request.actionType() == ActionType.EMAIL || request.actionType() == ActionType.WHATSAPP) {
            if (request.templateId() == null && (request.messageBody() == null || request.messageBody().isBlank())) {
                throw new BusinessException("Step of type " + request.actionType() + " requires a template or message body");
            }
        }
    }

    private Campaign findCampaignForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
        if (!campaign.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Campaign tenant does not match");
        }
        return campaign;
    }

    private EmailTemplate findTemplateForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        EmailTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("EmailTemplate not found: " + id));
        if (!t.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Template tenant does not match");
        }
        return t;
    }

    private SequenceStep toStepEntity(SequenceStep step, SequenceStepRequest req, UUID sequenceId, UUID tenantId) {
        step.setTenantId(tenantId);
        step.setSequenceId(sequenceId);
        step.setStepOrder(req.stepOrder());
        step.setDelayMinutes(req.delayMinutes());
        step.setActionType(req.actionType());
        step.setTemplateId(req.templateId());
        step.setMessageBody(req.messageBody());
        return step;
    }

    CampaignResponse toResponse(Campaign campaign) {
        List<SequenceResponse> sequences = sequenceRepository.findAllByCampaignId(campaign.getId()).stream()
                .map(this::toSequenceResponse).toList();
        return new CampaignResponse(campaign.getId(), campaign.getTenantId(),
                campaign.getName(), campaign.getDescription(), campaign.getStatus(), sequences);
    }

    SequenceResponse toSequenceResponse(Sequence sequence) {
        List<SequenceStepResponse> steps = stepRepository
                .findAllBySequenceIdOrderByStepOrder(sequence.getId()).stream()
                .map(this::toStepResponse).toList();
        return new SequenceResponse(sequence.getId(), sequence.getTenantId(),
                sequence.getCampaignId(), sequence.getName(), steps);
    }

    SequenceStepResponse toStepResponse(SequenceStep step) {
        return new SequenceStepResponse(step.getId(), step.getSequenceId(),
                step.getStepOrder(), step.getDelayMinutes(),
                step.getActionType(), step.getTemplateId(), step.getMessageBody());
    }

    EmailTemplateResponse toTemplateResponse(EmailTemplate t) {
        return new EmailTemplateResponse(t.getId(), t.getTenantId(), t.getName(), t.getSubject(), t.getBody());
    }
}
