package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.Sequence;
import java.util.List;
import java.util.UUID;

public interface SequenceRepository extends BaseRepository<Sequence> {
    List<Sequence> findAllByCampaignId(UUID campaignId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
