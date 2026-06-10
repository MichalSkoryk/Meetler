ALTER TABLE app_group
    ADD COLUMN event_requires_confirmation BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE group_event (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES app_group(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(255) NOT NULL,
    requires_confirmation BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_group_event_time CHECK (ends_at > starts_at),
    CONSTRAINT ck_group_event_status CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_group_event_group_time
    ON group_event (group_id, starts_at, ends_at);

CREATE TABLE group_event_participant (
    id UUID PRIMARY KEY,
    group_event_id UUID NOT NULL REFERENCES group_event(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status VARCHAR(255) NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_group_event_participant_event_user UNIQUE (group_event_id, user_id),
    CONSTRAINT ck_group_event_participant_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
);

CREATE INDEX idx_group_event_participant_event
    ON group_event_participant (group_event_id);

CREATE INDEX idx_group_event_participant_user
    ON group_event_participant (user_id);
