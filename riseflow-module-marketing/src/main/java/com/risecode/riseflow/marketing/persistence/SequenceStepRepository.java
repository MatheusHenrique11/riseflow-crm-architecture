package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.SequenceStep;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SequenceStepRepository extends BaseRepository<SequenceStep> {
    List<SequenceStep> findAllBySequenceIdOrderByStepOrder(UUID sequenceId);
    boolean existsBySequenceIdAndStepOrder(UUID sequenceId, int stepOrder);
    boolean existsBySequenceIdAndStepOrderAndIdNot(UUID sequenceId, int stepOrder, UUID id);
    Optional<SequenceStep> findBySequenceIdAndStepOrder(UUID sequenceId, int stepOrder);
}
