alter table group_invite
    add column revoked_at timestamp with time zone,
    add column revoked_by_user_id uuid references app_user(id);
