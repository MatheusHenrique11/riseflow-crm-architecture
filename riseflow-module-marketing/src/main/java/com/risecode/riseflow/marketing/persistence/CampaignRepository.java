package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.Campaign;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends BaseRepository<Campaign> {
    List<Campaign> findAllByTenantId(UUID tenantId);
}
