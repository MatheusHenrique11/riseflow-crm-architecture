package com.risecode.riseflow.accounts.customfields.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomFieldDefinitionServiceTest {

    @Mock
    private CustomFieldDefinitionRepository repository;

    @InjectMocks
    private CustomFieldDefinitionService service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateDefinitionForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(repository.existsByTenantIdAndEntityTypeAndFieldName(tenantId, "ACCOUNT", "revenue")).thenReturn(false);
        when(repository.save(any(CustomFieldDefinition.class))).thenAnswer(inv -> {
            CustomFieldDefinition d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        CustomFieldDefinitionResponse response = service.createDefinition(
                new CustomFieldDefinitionRequest("ACCOUNT", "revenue", FieldType.NUMBER, null, false, 1));

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.fieldName()).isEqualTo("revenue");
        assertThat(response.fieldType()).isEqualTo(FieldType.NUMBER);
    }

    @Test
    void shouldRejectDuplicateFieldNameForSameTenantAndEntityType() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(repository.existsByTenantIdAndEntityTypeAndFieldName(tenantId, "ACCOUNT", "revenue")).thenReturn(true);

        assertThatThrownBy(() ->
                service.createDefinition(new CustomFieldDefinitionRequest("ACCOUNT", "revenue", FieldType.NUMBER, null, false, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revenue");
    }

    @Test
    void shouldRequirePicklistOptionsWhenTypeIsPicklist() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        assertThatThrownBy(() ->
                service.createDefinition(new CustomFieldDefinitionRequest("ACCOUNT", "tier", FieldType.PICKLIST, null, false, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("picklistOptions");
    }

    @Test
    void shouldRequireNonEmptyPicklistOptionsWhenTypeIsPicklist() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        assertThatThrownBy(() ->
                service.createDefinition(new CustomFieldDefinitionRequest("ACCOUNT", "tier", FieldType.PICKLIST, List.of(), false, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("picklistOptions");
    }

    @Test
    void shouldRejectPicklistOptionsForNonPicklistType() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        assertThatThrownBy(() ->
                service.createDefinition(new CustomFieldDefinitionRequest("ACCOUNT", "revenue", FieldType.NUMBER, List.of("A", "B"), false, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("picklistOptions");
    }

    @Test
    void shouldReturnDefinitionsForCurrentTenantAndEntityType() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        CustomFieldDefinition def = definition(UUID.randomUUID(), tenantId, "revenue", FieldType.NUMBER);
        when(repository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT")).thenReturn(List.of(def));

        List<CustomFieldDefinitionResponse> result = service.getDefinitionsForTenant("ACCOUNT");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fieldName()).isEqualTo("revenue");
    }

    @Test
    void shouldDeleteDefinitionForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        CustomFieldDefinition def = definition(defId, tenantId, "revenue", FieldType.NUMBER);
        when(repository.findById(defId)).thenReturn(Optional.of(def));

        service.deleteDefinition(defId);

        verify(repository).delete(def);
    }

    @Test
    void shouldThrowWhenDefinitionBelongsToAnotherTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantA);
        when(repository.findById(defId)).thenReturn(Optional.of(definition(defId, tenantB, "revenue", FieldType.NUMBER)));

        assertThatThrownBy(() -> service.deleteDefinition(defId)).isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void shouldThrowWhenDefinitionNotFound() {
        TenantContextHolder.setTenantId(UUID.randomUUID());
        UUID defId = UUID.randomUUID();
        when(repository.findById(defId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDefinition(defId)).isInstanceOf(EntityNotFoundException.class);
    }

    private CustomFieldDefinition definition(UUID id, UUID tenantId, String fieldName, FieldType fieldType) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setId(id);
        def.setTenantId(tenantId);
        def.setEntityType("ACCOUNT");
        def.setFieldName(fieldName);
        def.setFieldType(fieldType);
        def.setDisplayOrder(0);
        return def;
    }
}
