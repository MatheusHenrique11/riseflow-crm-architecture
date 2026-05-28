package com.risecode.riseflow.accounts.persistence;

import com.risecode.riseflow.accounts.domain.Account;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;

public interface AccountRepository extends BaseRepository<Account> {
    List<Account> findAllByTenantId(UUID tenantId);
}
