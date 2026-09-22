package com.risecode.riseflow.core.service;

import com.risecode.riseflow.core.domain.TenantAwareEntity;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.repository.BaseRepository;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseService<T extends TenantAwareEntity> {
    private final BaseRepository<T> repository;

    protected BaseService(BaseRepository<T> repository) {
        this.repository = repository;
    }

    @Transactional
    public T create(T entity) {
        validateTenant(entity);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public T get(UUID id) {
        T entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + id));
        validateTenant(entity);
        return entity;
    }

    @Transactional(readOnly = true)
    public List<T> list() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return repository.findAll(byTenantId(tenantId));
    }

    private Specification<T> byTenantId(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    @Transactional
    public T update(UUID id, T replacement) {
        T current = get(id);
        validateTenant(replacement);
        replacement.setId(current.getId());
        return repository.save(replacement);
    }

    @Transactional
    public void delete(UUID id) {
        T entity = get(id);
        repository.delete(entity);
    }

    protected void validateTenant(T entity) {
        UUID currentTenantId = TenantContextHolder.requireTenantId();
        if (!currentTenantId.equals(entity.getTenantId())) {
            throw new TenantMismatchException("Entity tenant does not match active tenant");
        }
    }
}
