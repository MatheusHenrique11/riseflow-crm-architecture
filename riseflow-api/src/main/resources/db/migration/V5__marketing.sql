CREATE TABLE email_templates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE sequences (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE sequence_steps (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sequence_id UUID NOT NULL REFERENCES sequences(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    delay_minutes INTEGER NOT NULL DEFAULT 0,
    action_type VARCHAR(20) NOT NULL,
    template_id UUID REFERENCES email_templates(id) ON DELETE SET NULL,
    message_body TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_step_order_per_sequence UNIQUE (sequence_id, step_order)
);

CREATE TABLE automation_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    trigger_type VARCHAR(40) NOT NULL,
    trigger_config JSONB,
    action_type VARCHAR(40) NOT NULL DEFAULT 'ENROLL_IN_SEQUENCE',
    action_config JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE sequence_enrollments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    sequence_id UUID NOT NULL REFERENCES sequences(id) ON DELETE CASCADE,
    current_step_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    contact_email VARCHAR(180),
    next_execution_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_email_templates_tenant ON email_templates(tenant_id);
CREATE INDEX idx_campaigns_tenant ON campaigns(tenant_id);
CREATE INDEX idx_sequences_campaign ON sequences(campaign_id);
CREATE INDEX idx_sequence_steps_sequence ON sequence_steps(sequence_id);
CREATE INDEX idx_automation_rules_tenant ON automation_rules(tenant_id);
CREATE INDEX idx_enrollments_tenant_status ON sequence_enrollments(tenant_id, status);
CREATE INDEX idx_enrollments_next_exec ON sequence_enrollments(next_execution_time) WHERE status = 'ACTIVE';
