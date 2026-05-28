package com.risecode.riseflow.deals.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.deals.domain.Pipeline;
import java.util.List;
import java.util.UUID;

public interface PipelineRepository extends BaseRepository<Pipeline> {
    List<Pipeline> findAllByTenantId(UUID tenantId);
}
