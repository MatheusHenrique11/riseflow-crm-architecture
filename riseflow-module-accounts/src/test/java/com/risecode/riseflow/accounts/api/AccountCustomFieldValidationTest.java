package com.risecode.riseflow.accounts.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.accounts.persistence.AccountRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountCustomFieldValidationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPassWhenRequiredCustomFieldIsPresent() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(requiredDef(tenantId, "revenue", FieldType.NUMBER)));

        assertThatCode(() ->
                accountService.validateCustomFields(tenantId, Map.of("revenue", 50000)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenRequiredCustomFieldIsMissing() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(requiredDef(tenantId, "revenue", FieldType.NUMBER)));

        assertThatThrownBy(() ->
                accountService.validateCustomFields(tenantId, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revenue");
    }

    @Test
    void shouldThrowWhenNumberFieldContainsNonNumericValue() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(optionalDef(tenantId, "revenue", FieldType.NUMBER)));

        assertThatThrownBy(() ->
                accountService.validateCustomFields(tenantId, Map.of("revenue", "not-a-number")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revenue");
    }

    @Test
    void shouldThrowWhenPicklistValueIsNotInOptions() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        CustomFieldDefinition def = optionalDef(tenantId, "tier", FieldType.PICKLIST);
        def.setPicklistOptions(List.of("Starter", "Growth", "Enterprise"));
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(def));

        assertThatThrownBy(() ->
                accountService.validateCustomFields(tenantId, Map.of("tier", "Unknown")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tier");
    }

    @Test
    void shouldPassWhenOptionalCustomFieldIsAbsent() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(optionalDef(tenantId, "revenue", FieldType.NUMBER)));

        assertThatCode(() ->
                accountService.validateCustomFields(tenantId, Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenBooleanFieldContainsInvalidValue() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(optionalDef(tenantId, "isVip", FieldType.BOOLEAN)));

        assertThatThrownBy(() ->
                accountService.validateCustomFields(tenantId, Map.of("isVip", "maybe")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("isVip");
    }

    @Test
    void shouldPassForValidPicklistValue() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        CustomFieldDefinition def = optionalDef(tenantId, "tier", FieldType.PICKLIST);
        def.setPicklistOptions(List.of("Starter", "Growth", "Enterprise"));
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of(def));

        assertThatCode(() ->
                accountService.validateCustomFields(tenantId, Map.of("tier", "Growth")))
                .doesNotThrowAnyException();
    }

    private CustomFieldDefinition requiredDef(UUID tenantId, String fieldName, FieldType fieldType) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setId(UUID.randomUUID());
        def.setTenantId(tenantId);
        def.setEntityType("ACCOUNT");
        def.setFieldName(fieldName);
        def.setFieldType(fieldType);
        def.setRequired(true);
        return def;
    }

    private CustomFieldDefinition optionalDef(UUID tenantId, String fieldName, FieldType fieldType) {
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setId(UUID.randomUUID());
        def.setTenantId(tenantId);
        def.setEntityType("ACCOUNT");
        def.setFieldName(fieldName);
        def.setFieldType(fieldType);
        def.setRequired(false);
        return def;
    }
}
