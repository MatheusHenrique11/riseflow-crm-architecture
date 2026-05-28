package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.AutomationRule;
import com.risecode.riseflow.marketing.domain.TriggerType;
import java.util.List;
import java.util.UUID;

public interface AutomationRuleRepository extends BaseRepository<AutomationRule> {
    List<AutomationRule> findAllByTenantId(UUID tenantId);
    List<AutomationRule> findAllByTenantIdAndTriggerTypeAndActiveTrue(UUID tenantId, TriggerType triggerType);
}
