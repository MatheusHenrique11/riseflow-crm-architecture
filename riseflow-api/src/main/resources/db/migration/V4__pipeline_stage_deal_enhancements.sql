ALTER TABLE deals ALTER COLUMN currency TYPE VARCHAR(3);

ALTER TABLE pipelines
    ADD COLUMN description TEXT,
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE stages
    ADD COLUMN probability INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN color VARCHAR(10);

ALTER TABLE stages
    ADD CONSTRAINT chk_stages_probability CHECK (probability >= 0 AND probability <= 100);

ALTER TABLE deals
    ADD COLUMN probability INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN expected_close_date DATE,
    ADD COLUMN actual_close_date DATE,
    ADD COLUMN responsible_id UUID,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN notes TEXT;

CREATE INDEX idx_deals_stage_id ON deals(stage_id);
CREATE INDEX idx_deals_pipeline_id ON deals(pipeline_id);
CREATE INDEX idx_deals_responsible_id ON deals(responsible_id);
