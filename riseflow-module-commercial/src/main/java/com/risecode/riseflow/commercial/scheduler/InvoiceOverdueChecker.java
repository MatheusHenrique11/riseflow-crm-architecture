package com.risecode.riseflow.commercial.scheduler;

import com.risecode.riseflow.commercial.api.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvoiceOverdueChecker {

    private static final Logger log = LoggerFactory.getLogger(InvoiceOverdueChecker.class);

    private final InvoiceService invoiceService;

    public InvoiceOverdueChecker(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkOverdueInvoices() {
        int count = invoiceService.markOverdueInvoices();
        log.info("Marked {} invoices as OVERDUE", count);
    }
}
