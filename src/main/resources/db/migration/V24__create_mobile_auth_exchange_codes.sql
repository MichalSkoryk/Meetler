create table mobile_auth_exchange_code (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    code_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);

create index idx_mobile_auth_exchange_expiry
    on mobile_auth_exchange_code(expires_at)
    where used_at is null;
