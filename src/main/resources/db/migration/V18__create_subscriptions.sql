CREATE TABLE IF NOT EXISTS subscription_plan (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    max_owned_groups INTEGER NOT NULL,
    max_availability_templates INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_subscription_plan_owned_groups CHECK (max_owned_groups >= 0),
    CONSTRAINT ck_subscription_plan_availability_templates CHECK (max_availability_templates >= 0)
);

CREATE TABLE IF NOT EXISTS user_subscription (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plan(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_user_subscription_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_user_subscription_expires_after_start CHECK (
        expires_at IS NULL OR expires_at > started_at
    )
);

CREATE INDEX IF NOT EXISTS idx_user_subscription_user_status
    ON user_subscription (user_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_subscription_one_active
    ON user_subscription (user_id)
    WHERE status = 'ACTIVE';

INSERT INTO subscription_plan (
    id,
    code,
    name,
    max_owned_groups,
    max_availability_templates,
    is_active,
    created_at,
    updated_at
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'FREE',
        'Free',
        1,
        1,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'PLUS',
        'Plus',
        5,
        5,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'PRO',
        'Pro',
        25,
        25,
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (code) DO NOTHING;
