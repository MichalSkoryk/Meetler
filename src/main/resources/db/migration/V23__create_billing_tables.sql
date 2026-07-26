CREATE TABLE billing_customer (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_user(id) ON DELETE CASCADE,
    revenuecat_app_user_id VARCHAR(255) NOT NULL UNIQUE,
    management_url TEXT,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE billing_entitlement (
    id UUID PRIMARY KEY,
    billing_customer_id UUID NOT NULL REFERENCES billing_customer(id) ON DELETE CASCADE,
    entitlement_id VARCHAR(128) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    plan_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    store VARCHAR(64),
    purchased_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    grace_period_expires_at TIMESTAMP WITH TIME ZONE,
    will_renew BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_billing_entitlement_plan CHECK (plan_code IN ('PLUS', 'PRO')),
    CONSTRAINT ck_billing_entitlement_status CHECK (
        status IN ('ACTIVE', 'CANCELLED', 'GRACE_PERIOD', 'BILLING_ISSUE', 'EXPIRED')
    ),
    CONSTRAINT ck_billing_entitlement_environment CHECK (
        environment IN ('SANDBOX', 'PRODUCTION')
    ),
    CONSTRAINT uq_billing_entitlement_source UNIQUE (
        billing_customer_id,
        entitlement_id,
        product_id,
        environment
    )
);

CREATE INDEX idx_billing_entitlement_customer_status
    ON billing_entitlement (billing_customer_id, environment, status);

CREATE TABLE billing_webhook_event (
    id UUID PRIMARY KEY,
    revenuecat_event_id VARCHAR(255) NOT NULL UNIQUE,
    app_user_id VARCHAR(255),
    event_type VARCHAR(64) NOT NULL,
    environment VARCHAR(16),
    payload TEXT NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

