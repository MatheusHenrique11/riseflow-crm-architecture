package com.risecode.riseflow.accounts.events;

import java.util.UUID;

public class AccountCreatedEvent {

    private final UUID accountId;
    private final UUID tenantId;
    private final String email;

    public AccountCreatedEvent(UUID accountId, UUID tenantId, String email) {
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.email = email;
    }

    public UUID accountId() { return accountId; }
    public UUID tenantId() { return tenantId; }
    public String email() { return email; }
}
