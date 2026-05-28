package com.risecode.riseflow.tenant.api;

import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.tenant.domain.Tenant;
import com.risecode.riseflow.tenant.persistence.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        Tenant tenant = toEntity(new Tenant(), request);
        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return tenantRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TenantResponse update(UUID id, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
        return toResponse(tenantRepository.save(toEntity(tenant, request)));
    }

    @Transactional
    public void delete(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
        tenantRepository.delete(tenant);
    }

    private Tenant toEntity(Tenant tenant, TenantRequest request) {
        tenant.setName(request.name());
        tenant.setDomain(request.domain());
        tenant.setOwnerEmail(request.ownerEmail());
        tenant.setActive(true);
        return tenant;
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getDomain(), tenant.getOwnerEmail(), tenant.isActive());
    }
}
