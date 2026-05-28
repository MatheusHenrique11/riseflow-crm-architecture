package com.risecode.riseflow.marketing.sender;

public interface EmailSender {
    void sendEmail(String to, String subject, String body);
}
