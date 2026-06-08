ALTER TABLE availability_template
    DROP CONSTRAINT ck_availability_template_default_availability_status;

UPDATE availability_template
SET default_availability_status = 'BUSY'
WHERE default_availability_status = 'UNAVAILABLE';

ALTER TABLE availability_template
    ADD CONSTRAINT ck_availability_template_default_availability_status
        CHECK (default_availability_status IN ('AVAILABLE', 'BUSY'));

ALTER TABLE availability_template_block
    DROP CONSTRAINT ck_availability_template_block_status;

UPDATE availability_template_block
SET status = 'BUSY'
WHERE status = 'UNAVAILABLE';

ALTER TABLE availability_template_block
    ADD CONSTRAINT ck_availability_template_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY'));

ALTER TABLE availability_template_recurring_block
    DROP CONSTRAINT ck_availability_template_recurring_block_status;

UPDATE availability_template_recurring_block
SET status = 'BUSY'
WHERE status = 'UNAVAILABLE';

ALTER TABLE availability_template_recurring_block
    ADD CONSTRAINT ck_availability_template_recurring_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY'));
