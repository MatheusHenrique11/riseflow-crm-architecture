package com.risecode.riseflow.commercial.persistence;

import com.risecode.riseflow.commercial.domain.Proposal;
import com.risecode.riseflow.commercial.domain.ProposalStatus;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends BaseRepository<Proposal> {

    List<Proposal> findAllByTenantId(UUID tenantId);

    List<Proposal> findAllByTenantIdAndStatus(UUID tenantId, ProposalStatus status);

    @Query("SELECT p FROM Proposal p WHERE p.tenantId = :tenantId AND (:accountId IS NULL OR p.accountId = :accountId) AND (:dealId IS NULL OR p.dealId = :dealId) AND (:status IS NULL OR p.status = :status)")
    List<Proposal> findFiltered(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId, @Param("dealId") UUID dealId, @Param("status") ProposalStatus status);
}
