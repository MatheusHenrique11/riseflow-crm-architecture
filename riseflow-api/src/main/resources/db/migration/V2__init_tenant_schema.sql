CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    industry VARCHAR(160),
    email VARCHAR(180),
    phone VARCHAR(40),
    custom_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(40),
    custom_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    contact_id UUID REFERENCES contacts(id) ON DELETE SET NULL,
    type VARCHAR(60) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    custom_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE pipelines (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE stages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    pipeline_id UUID NOT NULL REFERENCES pipelines(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE deals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    pipeline_id UUID NOT NULL REFERENCES pipelines(id) ON DELETE RESTRICT,
    stage_id UUID NOT NULL REFERENCES stages(id) ON DELETE RESTRICT,
    title VARCHAR(160) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    custom_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_accounts_tenant_id ON accounts(tenant_id);
CREATE INDEX idx_contacts_tenant_id ON contacts(tenant_id);
CREATE INDEX idx_activities_tenant_id ON activities(tenant_id);
CREATE INDEX idx_deals_tenant_id ON deals(tenant_id);
