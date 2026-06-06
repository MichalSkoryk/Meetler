ALTER TABLE availability_template_recurring_block
    ADD COLUMN frequency VARCHAR(16) NOT NULL DEFAULT 'WEEKLY',
    ADD COLUMN interval_count INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN day_of_month INTEGER,
    ADD COLUMN month_of_year INTEGER,
    ADD COLUMN starts_on DATE,
    ADD COLUMN ends_on DATE;

ALTER TABLE availability_template_recurring_block
    ALTER COLUMN frequency DROP DEFAULT,
    ALTER COLUMN interval_count DROP DEFAULT,
    ALTER COLUMN day_of_week DROP NOT NULL;

ALTER TABLE availability_template_recurring_block
    ADD CONSTRAINT ck_availability_template_recurring_block_frequency
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    ADD CONSTRAINT ck_availability_template_recurring_block_interval
        CHECK (interval_count >= 1),
    ADD CONSTRAINT ck_availability_template_recurring_block_day_of_month
        CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31),
    ADD CONSTRAINT ck_availability_template_recurring_block_month_of_year
        CHECK (month_of_year IS NULL OR month_of_year BETWEEN 1 AND 12),
    ADD CONSTRAINT ck_availability_template_recurring_block_date_range
        CHECK (ends_on IS NULL OR starts_on IS NULL OR ends_on >= starts_on),
    ADD CONSTRAINT ck_availability_template_recurring_block_shape
        CHECK (
            (frequency = 'DAILY'
                AND day_of_week IS NULL
                AND day_of_month IS NULL
                AND month_of_year IS NULL)
            OR
            (frequency = 'WEEKLY'
                AND day_of_week IS NOT NULL
                AND day_of_month IS NULL
                AND month_of_year IS NULL)
            OR
            (frequency = 'MONTHLY'
                AND day_of_week IS NULL
                AND day_of_month IS NOT NULL
                AND month_of_year IS NULL)
            OR
            (frequency = 'YEARLY'
                AND day_of_week IS NULL
                AND day_of_month IS NOT NULL
                AND month_of_year IS NOT NULL)
        );

CREATE INDEX idx_availability_template_recurring_block_frequency_time
    ON availability_template_recurring_block (
        template_id,
        frequency,
        day_of_week,
        month_of_year,
        day_of_month,
        start_time,
        end_time
    );
