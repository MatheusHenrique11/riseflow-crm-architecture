package com.risecode.riseflow.deals.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealCustomFieldValidationTest {

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @InjectMocks
    private DealService dealService;

    @Mock
    private com.risecode.riseflow.deals.persistence.DealRepository dealRepository;

    @Mock
    private com.risecode.riseflow.deals.persistence.PipelineRepository pipelineRepository;

    @Mock
    private com.risecode.riseflow.deals.persistence.StageRepository stageRepository;

    @Test
    void shouldPassValidation_whenNoDefinitionsDeclared() {
        UUID tenantId = UUID.randomUUID();
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of());

        assertThatCode(() -> dealService.validateDealCustomFields(tenantId, Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPassValidation_whenAllRequiredFieldsPresent() {
        UUID tenantId = UUID.randomUUID();
        CustomFieldDefinition def = requiredDef("closeReason", FieldType.TEXT);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of(def));

        assertThatCode(() -> dealService.validateDealCustomFields(tenantId, Map.of("closeReason", "Signed")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDeal_whenRequiredCustomFieldMissing() {
        UUID tenantId = UUID.randomUUID();
        CustomFieldDefinition def = requiredDef("closeReason", FieldType.TEXT);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of(def));

        assertThatThrownBy(() -> dealService.validateDealCustomFields(tenantId, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("closeReason");
    }

    @Test
    void shouldRejectDeal_whenNumberFieldIsNotNumeric() {
        UUID tenantId = UUID.randomUUID();
        CustomFieldDefinition def = optionalDef("score", FieldType.NUMBER);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of(def));

        assertThatThrownBy(() -> dealService.validateDealCustomFields(tenantId, Map.of("score", "not-a-number")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("score");
    }

    @Test
    void shouldPassValidation_whenNumberFieldIsValidNumber() {
        UUID tenantId = UUID.randomUUID();
        CustomFieldDefinition def = optionalDef("score", FieldType.NUMBER);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of(def));

        assertThatCode(() -> dealService.validateDealCustomFields(tenantId, Map.of("score", 42.5)))
                .doesNotThrowAnyException();
    }

    private CustomFieldDefinition requiredDef(String fieldName, FieldType fieldType) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setId(UUID.randomUUID());
        def.setFieldName(fieldName);
        def.setFieldType(fieldType);
        def.setRequired(true);
        def.setEntityType("DEAL");
        return def;
    }

    private CustomFieldDefinition optionalDef(String fieldName, FieldType fieldType) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setId(UUID.randomUUID());
        def.setFieldName(fieldName);
        def.setFieldType(fieldType);
        def.setRequired(false);
        def.setEntityType("DEAL");
        return def;
    }
}
