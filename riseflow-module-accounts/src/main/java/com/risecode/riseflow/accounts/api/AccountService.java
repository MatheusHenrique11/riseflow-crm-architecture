package com.risecode.riseflow.accounts.api;

import com.risecode.riseflow.accounts.customfields.domain.CustomFieldDefinition;
import com.risecode.riseflow.accounts.customfields.domain.FieldType;
import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.accounts.domain.Account;
import com.risecode.riseflow.accounts.events.AccountCreatedEvent;
import com.risecode.riseflow.accounts.persistence.AccountRepository;
import com.risecode.riseflow.accounts.search.AccountSpecification;
import com.risecode.riseflow.accounts.search.SearchCriteria;
import com.risecode.riseflow.core.dto.PageResponse;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(AccountRepository accountRepository,
            CustomFieldDefinitionRepository customFieldDefinitionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Map<String, Object> customFields = request.customFields() != null ? request.customFields() : Map.of();
        validateCustomFields(tenantId, customFields);
        Account account = toEntity(new Account(), request);
        account.setTenantId(tenantId);
        Account saved = accountRepository.save(account);
        eventPublisher.publishEvent(new AccountCreatedEvent(saved.getId(), tenantId, saved.getEmail()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return toResponse(findForCurrentTenant(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return accountRepository.findAllByTenantId(TenantContextHolder.requireTenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Account account = findForCurrentTenant(id);
        Map<String, Object> customFields = request.customFields() != null ? request.customFields() : Map.of();
        validateCustomFields(tenantId, customFields);
        return toResponse(accountRepository.save(toEntity(account, request)));
    }

    @Transactional
    public void delete(UUID id) {
        accountRepository.delete(findForCurrentTenant(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> searchAccounts(List<SearchCriteria> criteria, int page, int size) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        Specification<Account> spec = AccountSpecification.fromCriteria(criteria, tenantId);
        Page<Account> result = accountRepository.findAll(spec, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    void validateCustomFields(UUID tenantId, Map<String, Object> customFields) {
        List<CustomFieldDefinition> definitions =
                customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT");

        for (CustomFieldDefinition def : definitions) {
            Object value = customFields.get(def.getFieldName());

            if (def.isRequired() && value == null) {
                throw new BusinessException("Required custom field missing: " + def.getFieldName());
            }

            if (value != null) {
                validateFieldValue(def, value);
            }
        }
    }

    private void validateFieldValue(CustomFieldDefinition def, Object value) {
        switch (def.getFieldType()) {
            case NUMBER -> {
                if (!(value instanceof Number)) {
                    try {
                        Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        throw new BusinessException(
                                "Custom field '" + def.getFieldName() + "' must be a number, got: " + value);
                    }
                }
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    String str = value.toString().toLowerCase();
                    if (!str.equals("true") && !str.equals("false")) {
                        throw new BusinessException(
                                "Custom field '" + def.getFieldName() + "' must be a boolean (true/false), got: " + value);
                    }
                }
            }
            case PICKLIST -> {
                String strValue = value.toString();
                if (def.getPicklistOptions() == null || !def.getPicklistOptions().contains(strValue)) {
                    throw new BusinessException(
                            "Custom field '" + def.getFieldName() + "' must be one of: " + def.getPicklistOptions()
                                    + ", got: " + strValue);
                }
            }
            default -> {
                // TEXT and DATE: any string is accepted
            }
        }
    }

    private Account findForCurrentTenant(UUID id) {
        UUID currentTenantId = TenantContextHolder.requireTenantId();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        if (!currentTenantId.equals(account.getTenantId())) {
            throw new TenantMismatchException("Account tenant does not match active tenant");
        }
        return account;
    }

    private Account toEntity(Account account, AccountRequest request) {
        account.setName(request.name());
        account.setIndustry(request.industry());
        account.setEmail(request.email());
        account.setPhone(request.phone());
        account.setCustomFields(request.customFields() != null ? new HashMap<>(request.customFields()) : new HashMap<>());
        return account;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getTenantId(),
                account.getName(),
                account.getIndustry(),
                account.getEmail(),
                account.getPhone(),
                account.getCustomFields());
    }
}
