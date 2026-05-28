package com.risecode.riseflow.marketing.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!production")
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[EMAIL] to={} subject={}", to, subject);
    }
}
