CREATE TABLE external_calendar_account (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(255) NOT NULL,
    external_account_id VARCHAR(255) NOT NULL,
    account_email VARCHAR(255),
    scopes TEXT,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,
    token_type VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_external_calendar_account_user_provider_external
        UNIQUE (user_id, provider, external_account_id)
);

CREATE INDEX idx_external_calendar_account_user_provider
    ON external_calendar_account (user_id, provider);
