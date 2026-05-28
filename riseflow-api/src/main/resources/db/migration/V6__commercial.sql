CREATE TABLE proposals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    deal_id UUID,
    account_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    valid_until DATE NOT NULL,
    total_value DECIMAL(15,2) NOT NULL DEFAULT 0,
    custom_fields JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    version BIGINT NOT NULL
);

CREATE TABLE proposal_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    proposal_id UUID NOT NULL REFERENCES proposals(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    order_no INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    account_id UUID NOT NULL,
    deal_id UUID,
    proposal_id UUID REFERENCES proposals(id) ON DELETE SET NULL,
    invoice_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(60),
    stripe_invoice_id VARCHAR(120),
    stripe_payment_intent_id VARCHAR(120),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_invoice_number_per_tenant UNIQUE (tenant_id, invoice_number)
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    order_no INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE commissions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    deal_id UUID NOT NULL,
    user_id UUID NOT NULL,
    proposal_id UUID,
    invoice_id UUID,
    percentage DECIMAL(5,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_proposals_tenant ON proposals(tenant_id);
CREATE INDEX idx_proposals_account ON proposals(account_id);
CREATE INDEX idx_proposals_deal ON proposals(deal_id);
CREATE INDEX idx_proposals_status ON proposals(tenant_id, status);
CREATE INDEX idx_proposal_items_proposal ON proposal_items(proposal_id);
CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_account ON invoices(account_id);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status = 'PENDING';
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_commissions_tenant ON commissions(tenant_id);
CREATE INDEX idx_commissions_deal ON commissions(deal_id);
CREATE INDEX idx_commissions_user ON commissions(user_id);
