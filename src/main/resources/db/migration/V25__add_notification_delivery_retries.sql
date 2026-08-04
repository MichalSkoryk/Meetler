alter table notification_delivery
    add column attempt_count integer not null default 0,
    add column last_attempt_at timestamptz,
    add column next_attempt_at timestamptz;

create index idx_notification_delivery_retry
    on notification_delivery(next_attempt_at, created_at)
    where status = 'PENDING';
