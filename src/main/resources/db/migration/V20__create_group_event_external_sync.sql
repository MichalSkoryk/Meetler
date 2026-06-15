CREATE TABLE group_event_external_sync (
    id UUID PRIMARY KEY,
    group_event_id UUID NOT NULL REFERENCES group_event(id) ON DELETE CASCADE,
    external_calendar_account_id UUID NOT NULL REFERENCES external_calendar_account(id) ON DELETE CASCADE,
    provider VARCHAR(255) NOT NULL,
    external_calendar_id VARCHAR(255) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_group_event_external_sync_event_account_provider
        UNIQUE (group_event_id, external_calendar_account_id, provider),
    CONSTRAINT ck_group_event_external_sync_provider
        CHECK (provider IN ('GOOGLE', 'MICROSOFT', 'INTERNAL')),
    CONSTRAINT ck_group_event_external_sync_status
        CHECK (status IN ('SYNCED', 'FAILED', 'DELETED'))
);

CREATE INDEX idx_group_event_external_sync_event
    ON group_event_external_sync (group_event_id);

CREATE INDEX idx_group_event_external_sync_account
    ON group_event_external_sync (external_calendar_account_id);
