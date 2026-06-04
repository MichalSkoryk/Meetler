CREATE TABLE availability_template (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_availability_template_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_availability_template_user
    ON availability_template (user_id);

CREATE UNIQUE INDEX uq_availability_template_default_per_user
    ON availability_template (user_id)
    WHERE is_default = TRUE;

CREATE TABLE availability_template_source_calendar (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES availability_template(id) ON DELETE CASCADE,
    calendar_id UUID NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
    include_busy_events BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_availability_template_source_calendar UNIQUE (template_id, calendar_id)
);

CREATE INDEX idx_availability_template_source_template
    ON availability_template_source_calendar (template_id);

CREATE INDEX idx_availability_template_source_calendar
    ON availability_template_source_calendar (calendar_id);

CREATE TABLE availability_template_block (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES availability_template(id) ON DELETE CASCADE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_availability_template_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY')),
    CONSTRAINT ck_availability_template_block_source
        CHECK (source IN ('MANUAL', 'IMPORTED', 'TEMPLATE')),
    CONSTRAINT ck_availability_template_block_time
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_availability_template_block_template
    ON availability_template_block (template_id);

CREATE INDEX idx_availability_template_block_time
    ON availability_template_block (template_id, starts_at, ends_at);
