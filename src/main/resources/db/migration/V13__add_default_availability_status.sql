ALTER TABLE availability_template
    ADD COLUMN default_availability_status VARCHAR(32) NOT NULL DEFAULT 'UNAVAILABLE';

ALTER TABLE availability_template
    ADD CONSTRAINT ck_availability_template_default_availability_status
        CHECK (default_availability_status IN ('AVAILABLE', 'UNAVAILABLE'));

ALTER TABLE availability_template
    ALTER COLUMN default_availability_status DROP DEFAULT;

ALTER TABLE availability_template_block
    DROP CONSTRAINT ck_availability_template_block_status,
    ADD CONSTRAINT ck_availability_template_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY', 'UNAVAILABLE'));

ALTER TABLE availability_template_recurring_block
    DROP CONSTRAINT ck_availability_template_recurring_block_status,
    ADD CONSTRAINT ck_availability_template_recurring_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY', 'UNAVAILABLE'));
