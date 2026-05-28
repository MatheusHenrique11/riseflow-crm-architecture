package com.risecode.riseflow.deals.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.deals.domain.Stage;
import java.util.List;
import java.util.UUID;

public interface StageRepository extends BaseRepository<Stage> {
    List<Stage> findAllByPipelineIdOrderByPosition(UUID pipelineId);
    boolean existsByPipelineIdAndPosition(UUID pipelineId, int position);
    boolean existsByPipelineIdAndPositionAndIdNot(UUID pipelineId, int position, UUID id);
}
