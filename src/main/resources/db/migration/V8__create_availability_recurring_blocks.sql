CREATE TABLE availability_template_recurring_block (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES availability_template(id) ON DELETE CASCADE,
    day_of_week VARCHAR(16) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_availability_template_recurring_block_day
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_availability_template_recurring_block_status
        CHECK (status IN ('AVAILABLE', 'BUSY')),
    CONSTRAINT ck_availability_template_recurring_block_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_availability_template_recurring_block_template
    ON availability_template_recurring_block (template_id);

CREATE INDEX idx_availability_template_recurring_block_day_time
    ON availability_template_recurring_block (template_id, day_of_week, start_time, end_time);
