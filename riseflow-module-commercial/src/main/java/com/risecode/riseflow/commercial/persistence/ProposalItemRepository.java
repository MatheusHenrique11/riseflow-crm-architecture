package com.risecode.riseflow.commercial.persistence;

import com.risecode.riseflow.commercial.domain.ProposalItem;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;

public interface ProposalItemRepository extends BaseRepository<ProposalItem> {

    List<ProposalItem> findAllByProposalIdOrderByOrderNo(UUID proposalId);

    void deleteAllByProposalId(UUID proposalId);
}
