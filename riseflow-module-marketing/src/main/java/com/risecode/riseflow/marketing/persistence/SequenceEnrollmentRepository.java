package com.risecode.riseflow.marketing.persistence;

import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.SequenceEnrollment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SequenceEnrollmentRepository extends BaseRepository<SequenceEnrollment> {
    List<SequenceEnrollment> findAllByStatusAndNextExecutionTimeBefore(EnrollmentStatus status, Instant cutoff);
    List<SequenceEnrollment> findAllByTenantId(UUID tenantId);
    List<SequenceEnrollment> findAllByTenantIdAndStatus(UUID tenantId, EnrollmentStatus status);
    boolean existsBySequenceIdAndStatus(UUID sequenceId, EnrollmentStatus status);
}
