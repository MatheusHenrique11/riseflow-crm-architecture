package com.risecode.riseflow.accounts.customfields.api;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomFieldDefinitionService {

    private final CustomFieldDefinitionRepository repository;

    public CustomFieldDefinitionService(CustomFieldDefinitionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomFieldDefinitionResponse createDefinition(CustomFieldDefinitionRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        validatePicklistConstraints(request.fieldType(), request.picklistOptions());
        if (repository.existsByTenantIdAndEntityTypeAndFieldName(tenantId, request.entityType(), request.fieldName())) {
            throw new BusinessException(
                    "Custom field '" + request.fieldName() + "' already exists for entity type '" + request.entityType() + "'");
        }
        CustomFieldDefinition def = toEntity(new CustomFieldDefinition(), request);
        def.setTenantId(tenantId);
        return toResponse(repository.save(def));
    }

    @Transactional
    public CustomFieldDefinitionResponse updateDefinition(UUID id, CustomFieldDefinitionRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        CustomFieldDefinition def = findForCurrentTenant(id);
        validatePicklistConstraints(request.fieldType(), request.picklistOptions());
        if (repository.existsByTenantIdAndEntityTypeAndFieldNameAndIdNot(tenantId, request.entityType(), request.fieldName(), id)) {
            throw new BusinessException(
                    "Custom field '" + request.fieldName() + "' already exists for entity type '" + request.entityType() + "'");
        }
        return toResponse(repository.save(toEntity(def, request)));
    }

    @Transactional
    public void deleteDefinition(UUID id) {
        CustomFieldDefinition def = findForCurrentTenant(id);
        repository.delete(def);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinitionResponse> getDefinitionsForTenant(String entityType) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return repository.findAllByTenantIdAndEntityType(tenantId, entityType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomFieldDefinitionResponse getDefinition(UUID id) {
        return toResponse(findForCurrentTenant(id));
    }

    private CustomFieldDefinition findForCurrentTenant(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        CustomFieldDefinition def = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Custom field definition not found: " + id));
        if (!tenantId.equals(def.getTenantId())) {
            throw new TenantMismatchException("Custom field definition does not belong to current tenant");
        }
        return def;
    }

    private void validatePicklistConstraints(FieldType fieldType, List<String> picklistOptions) {
        if (fieldType == FieldType.PICKLIST && (picklistOptions == null || picklistOptions.isEmpty())) {
            throw new BusinessException("picklistOptions must not be empty when fieldType is PICKLIST");
        }
        if (fieldType != FieldType.PICKLIST && picklistOptions != null && !picklistOptions.isEmpty()) {
            throw new BusinessException("picklistOptions must be null when fieldType is not PICKLIST");
        }
    }

    private CustomFieldDefinition toEntity(CustomFieldDefinition def, CustomFieldDefinitionRequest request) {
        def.setEntityType(request.entityType());
        def.setFieldName(request.fieldName());
        def.setFieldType(request.fieldType());
        def.setPicklistOptions(request.picklistOptions());
        def.setRequired(request.required());
        def.setDisplayOrder(request.displayOrder());
        return def;
    }

    private CustomFieldDefinitionResponse toResponse(CustomFieldDefinition def) {
        return new CustomFieldDefinitionResponse(
                def.getId(),
                def.getTenantId(),
                def.getEntityType(),
                def.getFieldName(),
                def.getFieldType(),
                def.getPicklistOptions(),
                def.isRequired(),
                def.getDisplayOrder(),
                def.getCreatedAt(),
                def.getUpdatedAt());
    }
}
