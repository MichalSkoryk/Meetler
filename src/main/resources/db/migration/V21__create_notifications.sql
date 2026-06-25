create table notification (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    type varchar(80) not null,
    title varchar(160) not null,
    body varchar(1000) not null,
    group_id uuid references app_group(id) on delete cascade,
    event_id uuid references group_event(id) on delete cascade,
    read_at timestamptz,
    created_at timestamptz not null
);

create index idx_notification_user_created_at on notification(user_id, created_at desc);
create index idx_notification_user_unread on notification(user_id) where read_at is null;

create table user_device (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    platform varchar(30) not null,
    provider varchar(30) not null,
    token varchar(1000) not null,
    enabled boolean not null,
    last_seen_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_user_device_user_token unique (user_id, token),
    constraint user_device_platform_check check (platform in ('ANDROID', 'IOS', 'WEB')),
    constraint user_device_provider_check check (provider in ('FCM'))
);

create index idx_user_device_user_active on user_device(user_id) where enabled = true and revoked_at is null;

create table notification_delivery (
    id uuid primary key,
    notification_id uuid not null references notification(id) on delete cascade,
    device_id uuid references user_device(id) on delete set null,
    channel varchar(40) not null,
    status varchar(40) not null,
    last_error varchar(2000),
    sent_at timestamptz,
    created_at timestamptz not null,
    constraint notification_delivery_channel_check check (channel in ('PUSH')),
    constraint notification_delivery_status_check check (status in ('PENDING', 'SENT', 'SKIPPED', 'FAILED'))
);

create index idx_notification_delivery_notification on notification_delivery(notification_id);
create index idx_notification_delivery_status on notification_delivery(status);
