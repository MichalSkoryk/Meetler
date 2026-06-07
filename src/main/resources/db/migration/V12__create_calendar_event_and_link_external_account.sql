ALTER TABLE calendar
    ADD COLUMN external_calendar_account_id UUID REFERENCES external_calendar_account(id) ON DELETE SET NULL;

CREATE INDEX idx_calendar_external_account
    ON calendar (external_calendar_account_id);

CREATE TABLE calendar_event (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
    external_id VARCHAR(512) NOT NULL,
    title VARCHAR(512),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_busy BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_calendar_event_calendar_external UNIQUE (calendar_id, external_id),
    CONSTRAINT ck_calendar_event_time CHECK (ends_at > starts_at)
);

CREATE INDEX idx_calendar_event_calendar_time
    ON calendar_event (calendar_id, starts_at, ends_at);
