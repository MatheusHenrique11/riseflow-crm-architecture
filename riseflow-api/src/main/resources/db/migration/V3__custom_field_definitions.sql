CREATE TABLE custom_field_definitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    field_name VARCHAR(120) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    picklist_options JSONB,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_custom_field_per_tenant_entity UNIQUE (tenant_id, entity_type, field_name)
);

CREATE INDEX idx_cfd_tenant_entity ON custom_field_definitions(tenant_id, entity_type);
