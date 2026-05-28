package com.risecode.riseflow.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.events.AccountCreatedEvent;
import com.risecode.riseflow.accounts.persistence.AccountRepository;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.events.DealStageChangedEvent;
import com.risecode.riseflow.marketing.api.AutomationRuleRequest;
import com.risecode.riseflow.marketing.api.AutomationRuleResponse;
import com.risecode.riseflow.marketing.api.AutomationService;
import com.risecode.riseflow.marketing.domain.AutomationRule;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.SequenceEnrollment;
import com.risecode.riseflow.marketing.domain.TriggerType;
import com.risecode.riseflow.marketing.persistence.AutomationRuleRepository;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;
    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AutomationService automationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID sequenceId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private AutomationRule ruleFor(TriggerType type, Map<String, String> triggerConfig) {
        AutomationRule rule = new AutomationRule();
        rule.setId(UUID.randomUUID());
        rule.setTenantId(tenantId);
        rule.setName("Test rule");
        rule.setTriggerType(type);
        rule.setTriggerConfig(triggerConfig);
        rule.setActionType(AutomationRule.ActionRuleType.ENROLL_IN_SEQUENCE);
        rule.setActionConfig(Map.of("sequenceId", sequenceId.toString()));
        rule.setActive(true);
        return rule;
    }

    @Test
    void shouldEnrollOnAccountCreated() {
        AutomationRule rule = ruleFor(TriggerType.ACCOUNT_CREATED, null);
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.ACCOUNT_CREATED))
                .thenReturn(List.of(rule));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID accountId = UUID.randomUUID();
        automationService.onAccountCreated(new AccountCreatedEvent(accountId, tenantId, "test@example.com"));

        ArgumentCaptor<SequenceEnrollment> captor = ArgumentCaptor.forClass(SequenceEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        SequenceEnrollment enrollment = captor.getValue();
        assertThat(enrollment.getSequenceId()).isEqualTo(sequenceId);
        assertThat(enrollment.getContactEmail()).isEqualTo("test@example.com");
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getCurrentStepOrder()).isEqualTo(0);
    }

    @Test
    void shouldEnrollOnDealStageChangedWithoutStageFilter() {
        AutomationRule rule = ruleFor(TriggerType.DEAL_STAGE_CHANGED, null);
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.DEAL_STAGE_CHANGED))
                .thenReturn(List.of(rule));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID dealId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        automationService.onDealStageChanged(
                new DealStageChangedEvent(dealId, tenantId, UUID.randomUUID(), stageId, null, null));

        verify(enrollmentRepository, times(1)).save(any());
    }

    @Test
    void shouldEnrollOnDealStageChangedWhenStageMatches() {
        UUID targetStage = UUID.randomUUID();
        AutomationRule rule = ruleFor(TriggerType.DEAL_STAGE_CHANGED, Map.of("stageId", targetStage.toString()));
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.DEAL_STAGE_CHANGED))
                .thenReturn(List.of(rule));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        automationService.onDealStageChanged(
                new DealStageChangedEvent(UUID.randomUUID(), tenantId, UUID.randomUUID(), targetStage, null, null));

        verify(enrollmentRepository, times(1)).save(any());
    }

    @Test
    void shouldSkipEnrollmentWhenStageDoesNotMatch() {
        UUID targetStage = UUID.randomUUID();
        AutomationRule rule = ruleFor(TriggerType.DEAL_STAGE_CHANGED, Map.of("stageId", targetStage.toString()));
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.DEAL_STAGE_CHANGED))
                .thenReturn(List.of(rule));

        automationService.onDealStageChanged(
                new DealStageChangedEvent(UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(), null, null));

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void shouldCreateAutomationRule() {
        TenantContextHolder.setTenantId(tenantId);
        AutomationRule saved = ruleFor(TriggerType.ACCOUNT_CREATED, null);
        when(ruleRepository.save(any())).thenReturn(saved);

        AutomationRuleResponse response = automationService.createRule(
                new AutomationRuleRequest("Rule", TriggerType.ACCOUNT_CREATED, null, sequenceId, true));

        assertThat(response.name()).isEqualTo("Test rule");
        assertThat(response.triggerType()).isEqualTo(TriggerType.ACCOUNT_CREATED);
        assertThat(response.sequenceId()).isEqualTo(sequenceId);
    }

    @Test
    void shouldListRules() {
        TenantContextHolder.setTenantId(tenantId);
        when(ruleRepository.findAllByTenantId(tenantId)).thenReturn(List.of(ruleFor(TriggerType.ACCOUNT_CREATED, null)));

        List<AutomationRuleResponse> rules = automationService.listRules();

        assertThat(rules).hasSize(1);
    }

    @Test
    void shouldResolveAccountEmailForDealEnrollment() {
        UUID accountId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        AutomationRule rule = ruleFor(TriggerType.DEAL_STAGE_CHANGED, null);
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.DEAL_STAGE_CHANGED))
                .thenReturn(List.of(rule));

        com.risecode.riseflow.accounts.domain.Account account = new com.risecode.riseflow.accounts.domain.Account();
        account.setEmail("contact@company.com");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        automationService.onDealStageChanged(
                new DealStageChangedEvent(dealId, tenantId, UUID.randomUUID(), stageId, accountId, null));

        ArgumentCaptor<SequenceEnrollment> captor = ArgumentCaptor.forClass(SequenceEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getContactEmail()).isEqualTo("contact@company.com");
    }

    @Test
    void shouldNoEnrollmentWhenNoRulesMatch() {
        when(ruleRepository.findAllByTenantIdAndTriggerTypeAndActiveTrue(tenantId, TriggerType.ACCOUNT_CREATED))
                .thenReturn(List.of());

        automationService.onAccountCreated(new AccountCreatedEvent(UUID.randomUUID(), tenantId, "x@x.com"));

        verify(enrollmentRepository, never()).save(any());
    }
}
