CREATE TABLE user_auth_identity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(255) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_user_auth_identity_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_user_auth_identity_user
    ON user_auth_identity (user_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'app_user'
          AND column_name = 'auth_provider'
    ) THEN
        INSERT INTO user_auth_identity (
            id,
            user_id,
            provider,
            provider_user_id,
            provider_email,
            created_at,
            last_login_at
        )
        SELECT
            id,
            id,
            'INTERNAL',
            email,
            email,
            created_at,
            created_at
        FROM app_user
        WHERE auth_provider = 'INTERNAL'
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
