package com.risecode.riseflow.accounts.customfields.persistence;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;

public interface CustomFieldDefinitionRepository extends BaseRepository<CustomFieldDefinition> {

    List<CustomFieldDefinition> findAllByTenantIdAndEntityType(UUID tenantId, String entityType);

    boolean existsByTenantIdAndEntityTypeAndFieldName(UUID tenantId, String entityType, String fieldName);

    boolean existsByTenantIdAndEntityTypeAndFieldNameAndIdNot(UUID tenantId, String entityType, String fieldName, UUID id);
}
