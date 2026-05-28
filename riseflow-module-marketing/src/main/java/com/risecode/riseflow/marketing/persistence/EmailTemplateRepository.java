package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.EmailTemplate;
import java.util.List;
import java.util.UUID;

public interface EmailTemplateRepository extends BaseRepository<EmailTemplate> {
    List<EmailTemplate> findAllByTenantId(UUID tenantId);
}
