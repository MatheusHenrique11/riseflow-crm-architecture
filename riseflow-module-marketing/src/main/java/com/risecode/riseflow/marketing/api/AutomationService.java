package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.accounts.events.AccountCreatedEvent;
import com.risecode.riseflow.accounts.persistence.AccountRepository;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.events.DealStageChangedEvent;
import com.risecode.riseflow.marketing.domain.AutomationRule;
import com.risecode.riseflow.marketing.domain.EntityType;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.SequenceEnrollment;
import com.risecode.riseflow.marketing.domain.TriggerType;
import com.risecode.riseflow.marketing.persistence.AutomationRuleRepository;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);

    private final AutomationRuleRepository ruleRepository;
    private final SequenceEnrollmentRepository enrollmentRepository;
    private final AccountRepository accountRepository;

    public AutomationService(AutomationRuleRepository ruleRepository,
            SequenceEnrollmentRepository enrollmentRepository,
            AccountRepository accountRepository) {
        this.ruleRepository = ruleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accountRepository = accountRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountCreated(AccountCreatedEvent event) {
        UUID tenantId = event.tenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            List<AutomationRule> rules = ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(
                    tenantId, TriggerType.ACCOUNT_CREATED);
            for (AutomationRule rule : rules) {
                enroll(tenantId, EntityType.ACCOUNT, event.accountId(),
                        extractSequenceId(rule), event.email());
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDealStageChanged(DealStageChangedEvent event) {
        UUID tenantId = event.tenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            List<AutomationRule> rules = ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(
                    tenantId, TriggerType.DEAL_STAGE_CHANGED);
            for (AutomationRule rule : rules) {
                if (matchesDealStageRule(rule, event)) {
                    String email = resolveAccountEmail(event.accountId());
                    enroll(tenantId, EntityType.DEAL, event.dealId(),
                            extractSequenceId(rule), email);
                }
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Transactional
    public AutomationRuleResponse createRule(AutomationRuleRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        AutomationRule rule = new AutomationRule();
        rule.setTenantId(tenantId);
        applyRequest(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> listRules() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return ruleRepository.findAllByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AutomationRuleResponse getRule(UUID id) {
        return toResponse(findForTenant(id));
    }

    @Transactional
    public AutomationRuleResponse updateRule(UUID id, AutomationRuleRequest request) {
        AutomationRule rule = findForTenant(id);
        applyRequest(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID id) {
        ruleRepository.delete(findForTenant(id));
    }

    // --- Private helpers ---

    private boolean matchesDealStageRule(AutomationRule rule, DealStageChangedEvent event) {
        Map<String, String> config = rule.getTriggerConfig();
        if (config == null || !config.containsKey("stageId")) {
            return true;
        }
        String targetStageId = config.get("stageId");
        return targetStageId.equals(event.newStageId().toString());
    }

    private UUID extractSequenceId(AutomationRule rule) {
        Map<String, String> config = rule.getActionConfig();
        if (config == null || !config.containsKey("sequenceId")) {
            throw new IllegalStateException("Rule " + rule.getId() + " missing sequenceId in action_config");
        }
        return UUID.fromString(config.get("sequenceId"));
    }

    private String resolveAccountEmail(UUID accountId) {
        if (accountId == null) return null;
        return accountRepository.findById(accountId).map(a -> a.getEmail()).orElse(null);
    }

    private void enroll(UUID tenantId, EntityType entityType, UUID entityId, UUID sequenceId, String contactEmail) {
        SequenceEnrollment enrollment = new SequenceEnrollment();
        enrollment.setTenantId(tenantId);
        enrollment.setEntityType(entityType);
        enrollment.setEntityId(entityId);
        enrollment.setSequenceId(sequenceId);
        enrollment.setCurrentStepOrder(0);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setContactEmail(contactEmail);
        enrollment.setNextExecutionTime(Instant.now());
        enrollmentRepository.save(enrollment);
        log.info("Enrolled {} {} in sequence {}", entityType, entityId, sequenceId);
    }

    private void applyRequest(AutomationRule rule, AutomationRuleRequest request) {
        rule.setName(request.name());
        rule.setTriggerType(request.triggerType());
        rule.setTriggerConfig(request.triggerConfig());
        rule.setActionType(AutomationRule.ActionRuleType.ENROLL_IN_SEQUENCE);
        rule.setActionConfig(Map.of("sequenceId", request.sequenceId().toString()));
        rule.setActive(request.active());
    }

    private AutomationRule findForTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        AutomationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AutomationRule not found: " + id));
        if (!rule.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Rule tenant does not match");
        }
        return rule;
    }

    AutomationRuleResponse toResponse(AutomationRule rule) {
        UUID sequenceId = null;
        if (rule.getActionConfig() != null && rule.getActionConfig().containsKey("sequenceId")) {
            sequenceId = UUID.fromString(rule.getActionConfig().get("sequenceId"));
        }
        return new AutomationRuleResponse(rule.getId(), rule.getTenantId(), rule.getName(),
                rule.getTriggerType(), rule.getTriggerConfig(), sequenceId, rule.isActive());
    }
}
