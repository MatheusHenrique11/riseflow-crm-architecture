package com.risecode.riseflow.deals.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.deals.domain.Deal;
import java.util.UUID;

public interface DealRepository extends BaseRepository<Deal> {
    boolean existsByPipelineId(UUID pipelineId);
    boolean existsByStageId(UUID stageId);
}
