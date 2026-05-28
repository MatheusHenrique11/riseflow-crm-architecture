package com.risecode.riseflow.commercial.persistence;

import com.risecode.riseflow.commercial.domain.Invoice;
import com.risecode.riseflow.commercial.domain.InvoiceStatus;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends BaseRepository<Invoice> {

    List<Invoice> findAllByTenantId(UUID tenantId);

    List<Invoice> findAllByTenantIdAndStatus(UUID tenantId, InvoiceStatus status);

    List<Invoice> findAllByTenantIdAndAccountId(UUID tenantId, UUID accountId);

    long countByTenantId(UUID tenantId);

    boolean existsByProposalId(UUID proposalId);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND (:accountId IS NULL OR i.accountId = :accountId) AND (:status IS NULL OR i.status = :status)")
    List<Invoice> findFiltered(@Param("tenantId") UUID tenantId, @Param("accountId") UUID accountId, @Param("status") InvoiceStatus status);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = :overdue WHERE i.dueDate < :today AND i.status = :pending")
    int markOverdueInvoices(@Param("today") LocalDate today, @Param("pending") InvoiceStatus pending, @Param("overdue") InvoiceStatus overdue);
}
