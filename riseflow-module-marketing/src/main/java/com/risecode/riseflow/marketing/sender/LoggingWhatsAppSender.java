package com.risecode.riseflow.marketing.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!production")
public class LoggingWhatsAppSender implements WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingWhatsAppSender.class);

    @Override
    public void sendMessage(String to, String messageBody) {
        log.info("[WHATSAPP] to={} body={}", to, messageBody);
    }
}
