package com.risecode.riseflow.accounts.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.accounts.domain.Account;
import com.risecode.riseflow.accounts.persistence.AccountRepository;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateAccountWithValidData() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "ACCOUNT"))
                .thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(UUID.randomUUID());
            return account;
        });

        AccountResponse response = accountService.create(
                new AccountRequest("Acme", "Software", "hello@acme.test", "123", Map.of()));

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.name()).isEqualTo("Acme");
    }

    @Test
    void shouldReturnOnlyAccountsForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(accountRepository.findAllByTenantId(tenantId)).thenReturn(List.of(account(UUID.randomUUID(), tenantId)));

        assertThat(accountService.list()).hasSize(1).allMatch(account -> account.tenantId().equals(tenantId));
    }

    @Test
    void shouldThrowExceptionWhenAccountBelongsToAnotherTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantA);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account(accountId, tenantB)));

        assertThatThrownBy(() -> accountService.get(accountId)).isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void shouldThrowExceptionWhenAccountDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(accountId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeleteAccountForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId, tenantId);
        TenantContextHolder.setTenantId(tenantId);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        accountService.delete(accountId);

        verify(accountRepository).delete(account);
    }

    private Account account(UUID id, UUID tenantId) {
        Account account = new Account();
        account.setId(id);
        account.setTenantId(tenantId);
        account.setName("Acme");
        account.setIndustry("Software");
        account.setEmail("hello@acme.test");
        account.setPhone("123");
        return account;
    }
}
