package com.risecode.riseflow.commercial.persistence;

import com.risecode.riseflow.commercial.domain.InvoiceItem;
import com.risecode.riseflow.core.repository.BaseRepository;
import java.util.List;
import java.util.UUID;

public interface InvoiceItemRepository extends BaseRepository<InvoiceItem> {

    List<InvoiceItem> findAllByInvoiceIdOrderByOrderNo(UUID invoiceId);

    void deleteAllByInvoiceId(UUID invoiceId);
}
