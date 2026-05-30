CREATE TABLE app_user (
    id              UUID PRIMARY KEY,
    email           TEXT UNIQUE NOT NULL,
    name            TEXT,
    role            TEXT NOT NULL CHECK (role IN ('GUEST', 'USER', 'ADMIN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    upgraded_at     TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    password_hash   TEXT,
    auth_provider   TEXT NOT NULL CHECK (auth_provider IN ('INTERNAL','GOOGLE','MICROSOFT','MAGIC_LINK'))
);

CREATE TABLE calendar (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider        TEXT NOT NULL CHECK (provider IN ('google','teams','internal')),
    external_id     TEXT,
    name            TEXT NOT NULL,
    color           TEXT,
    is_editable     BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sync_direction  TEXT NOT NULL CHECK (sync_direction IN ('NONE','TO_PROVIDER','FROM_PROVIDER','BIDIRECTIONAL')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE calendar_event (
    id              UUID PRIMARY KEY,
    calendar_id     UUID NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
    start_at        TIMESTAMPTZ NOT NULL,
    end_at          TIMESTAMPTZ NOT NULL,
    busy            BOOLEAN NOT NULL DEFAULT TRUE,
    metadata        JSONB,
    time_range      TSRANGE GENERATED ALWAYS AS (tsrange(start_at, end_at, '[)')) STORED
);

CREATE INDEX idx_calendar_event_range
    ON calendar_event USING GIST (time_range);

CREATE TABLE user_availability_template (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_availability_template_source (
    id              UUID PRIMARY KEY,
    template_id     UUID NOT NULL REFERENCES user_availability_template(id) ON DELETE CASCADE,
    calendar_id     UUID NOT NULL REFERENCES calendar(id) ON DELETE CASCADE,
    include         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE availability_group (
    id                          UUID PRIMARY KEY,
    name                        TEXT NOT NULL,
    description                 TEXT,
    created_by                  UUID NOT NULL REFERENCES app_user(id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    min_event_duration_minutes  INT NOT NULL DEFAULT 30,
    min_required_people         INT NOT NULL DEFAULT 2
);

CREATE TABLE availability_group_member (
    id          UUID PRIMARY KEY,
    group_id    UUID NOT NULL REFERENCES availability_group(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role        TEXT NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);

CREATE TABLE group_user_calendar (
    id                          UUID PRIMARY KEY,
    group_id                    UUID NOT NULL REFERENCES availability_group(id) ON DELETE CASCADE,
    user_id                     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    calendar_type               TEXT NOT NULL CHECK (calendar_type IN ('TEMPLATE','COPY','CUSTOM')),
    calendar_origin_id          UUID REFERENCES group_user_calendar(id),

    calendar_lock_level         TEXT NOT NULL CHECK (calendar_lock_level IN ('READ_ONLY','PARTIAL','FULL')),
    affects_global_availability BOOLEAN NOT NULL DEFAULT TRUE,

    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE group_user_calendar_event (
    id                          UUID PRIMARY KEY,
    group_user_calendar_id      UUID NOT NULL REFERENCES group_user_calendar(id) ON DELETE CASCADE,

    start_at                    TIMESTAMPTZ NOT NULL,
    end_at                      TIMESTAMPTZ NOT NULL,
    busy                        BOOLEAN NOT NULL DEFAULT TRUE,
    metadata                    JSONB,

    time_range                  TSRANGE GENERATED ALWAYS AS (tsrange(start_at, end_at, '[)')) STORED,

    event_visibility_scope      TEXT NOT NULL CHECK (event_visibility_scope IN ('GROUP_ONLY','GLOBAL','TEMPLATE_ONLY')),
    sync_direction              TEXT NOT NULL CHECK (sync_direction IN ('NONE','TO_PROVIDER','FROM_PROVIDER','BIDIRECTIONAL')),

    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_group_user_calendar_event_range
    ON group_user_calendar_event USING GIST (time_range);

CREATE TABLE group_invite_code (
    id              UUID PRIMARY KEY,
    group_id        UUID NOT NULL REFERENCES availability_group(id) ON DELETE CASCADE,

    code_hash       TEXT NOT NULL,
    type            TEXT NOT NULL CHECK (type IN ('PERMANENT','TEMPORARY')),

    expires_at      TIMESTAMPTZ,
    max_uses        INT,
    uses            INT NOT NULL DEFAULT 0,

    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID NOT NULL REFERENCES app_user(id)
);

CREATE INDEX idx_group_invite_code_group
    ON group_invite_code (group_id);

CREATE INDEX idx_group_invite_code_active
    ON group_invite_code (is_active);

CREATE TABLE user_device (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    device_token    TEXT NOT NULL,
    platform        TEXT NOT NULL CHECK (platform IN ('ANDROID','IOS')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_notification_settings (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

    event_reminders         BOOLEAN NOT NULL DEFAULT TRUE,
    group_activity          BOOLEAN NOT NULL DEFAULT TRUE,
    system_notifications    BOOLEAN NOT NULL DEFAULT TRUE,

    push_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled           BOOLEAN NOT NULL DEFAULT FALSE,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription_plan (
    id                  UUID PRIMARY KEY,
    name                TEXT NOT NULL,
    description         TEXT,
    price_monthly       NUMERIC(10,2),
    price_yearly        NUMERIC(10,2),
    currency            TEXT NOT NULL DEFAULT 'USD',
    max_groups          INT,
    max_calendars       INT,
    max_members_per_group INT,
    max_ai_calls        INT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE subscription (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    plan_id                 UUID NOT NULL REFERENCES subscription_plan(id),
    status                  TEXT NOT NULL CHECK (status IN ('ACTIVE','CANCELED','EXPIRED','PAST_DUE')),
    start_at                TIMESTAMPTZ NOT NULL,
    end_at                  TIMESTAMPTZ NOT NULL,
    renews_at               TIMESTAMPTZ,
    cancel_at               TIMESTAMPTZ,
    provider                TEXT,
    provider_subscription_id TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoice (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    subscription_id     UUID REFERENCES subscription(id),
    amount              NUMERIC(10,2) NOT NULL,
    currency            TEXT NOT NULL,
    pdf_url             TEXT,
    issued_at           TIMESTAMPTZ NOT NULL,
    paid_at             TIMESTAMPTZ,
    status              TEXT NOT NULL CHECK (status IN ('ISSUED','PAID','VOID'))
);

CREATE TABLE refresh_token
(
    id         UUID                        NOT NULL,
    token      VARCHAR(255)                NOT NULL,
    user_id    UUID                        NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked    BOOLEAN                     NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id)
);

ALTER TABLE refresh_token
    ADD CONSTRAINT uc_refresh_token_token UNIQUE (token);

ALTER TABLE refresh_token
    ADD CONSTRAINT FK_REFRESH_TOKEN_ON_USER FOREIGN KEY (user_id) REFERENCES app_user (id);

CREATE TABLE app_group
(
    id         UUID                        NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    owner_id   UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_app_group PRIMARY KEY (id)
);


CREATE TABLE group_member (
                              id UUID PRIMARY KEY,
                              group_id UUID NOT NULL REFERENCES app_group(id) ON DELETE CASCADE,
                              user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                              role TEXT NOT NULL, -- MEMBER / ADMIN / OWNER
                              joined_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE group_invite (
                              id UUID PRIMARY KEY,
                              group_id UUID NOT NULL REFERENCES app_group(id) ON DELETE CASCADE,
                              code TEXT NOT NULL UNIQUE,
                              expires_at TIMESTAMPTZ,
                              max_uses INT,
                              uses INT NOT NULL DEFAULT 0,
                              created_at TIMESTAMPTZ NOT NULL
);

