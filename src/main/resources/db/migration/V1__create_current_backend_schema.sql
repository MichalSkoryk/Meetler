CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    upgraded_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    password_hash VARCHAR(255)
);

CREATE TABLE calendar (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    color VARCHAR(255),
    is_editable BOOLEAN NOT NULL,
    is_active BOOLEAN NOT NULL,
    external_id VARCHAR(255),
    sync_direction VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE app_group (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE group_member (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES app_group(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_group_member_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE group_invite (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES app_group(id),
    code VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    max_uses INTEGER,
    uses INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL
);
