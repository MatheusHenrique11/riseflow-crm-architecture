package com.risecode.riseflow.commercial.persistence;

import com.risecode.riseflow.commercial.domain.Commission;
import com.risecode.riseflow.commercial.domain.CommissionStatus;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionRepository extends BaseRepository<Commission> {

    List<Commission> findAllByTenantId(UUID tenantId);

    List<Commission> findAllByTenantIdAndDealIdAndStatus(UUID tenantId, UUID dealId, CommissionStatus status);

    @Query("SELECT c FROM Commission c WHERE c.tenantId = :tenantId AND (:userId IS NULL OR c.userId = :userId) AND (:dealId IS NULL OR c.dealId = :dealId) AND (:status IS NULL OR c.status = :status)")
    List<Commission> findFiltered(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("dealId") UUID dealId, @Param("status") CommissionStatus status);
}
