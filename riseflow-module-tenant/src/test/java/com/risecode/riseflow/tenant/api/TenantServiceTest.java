package com.risecode.riseflow.tenant.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.tenant.domain.Tenant;
import com.risecode.riseflow.tenant.persistence.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldCreateTenantWithValidData() {
        Tenant saved = tenant(UUID.randomUUID());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(saved);

        TenantResponse response = tenantService.create(new TenantRequest("Acme", "acme", "owner@acme.test"));

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.domain()).isEqualTo("acme");
    }

    @Test
    void shouldReturnTenantWhenIdExists() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant(id)));

        TenantResponse response = tenantService.get(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void shouldThrowExceptionWhenTenantDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.get(id)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldDeleteTenantWhenIdExists() {
        UUID id = UUID.randomUUID();
        Tenant tenant = tenant(id);
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        tenantService.delete(id);

        verify(tenantRepository).delete(tenant);
    }

    private Tenant tenant(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Acme");
        tenant.setDomain("acme");
        tenant.setOwnerEmail("owner@acme.test");
        tenant.setActive(true);
        return tenant;
    }
}
