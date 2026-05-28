package com.risecode.riseflow.commercial.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!production")
public class LoggingPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentGateway.class);

    @Override
    public PaymentResult createInvoice(String customerEmail, String description, BigDecimal amount, String currency) {
        String mockInvoiceId = "inv_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String mockIntentId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String mockSecret = mockIntentId + "_secret_mock";
        log.info("[MOCK] Creating payment invoice for {} | {} {} | desc: {}", customerEmail, amount, currency, description);
        return new PaymentResult(mockInvoiceId, mockIntentId, mockSecret, "open");
    }

    @Override
    public WebhookEvent handleWebhook(String payload, String signature) {
        log.info("[MOCK] Handling webhook payload (length={})", payload == null ? 0 : payload.length());
        return new WebhookEvent("invoice.paid", "inv_mock_unknown", "paid", BigDecimal.ZERO);
    }
}
