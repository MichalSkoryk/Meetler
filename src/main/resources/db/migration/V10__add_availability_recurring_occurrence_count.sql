ALTER TABLE availability_template_recurring_block
    ADD COLUMN occurrence_count INTEGER;

ALTER TABLE availability_template_recurring_block
    ADD CONSTRAINT ck_availability_template_recurring_block_occurrence_count
        CHECK (occurrence_count IS NULL OR occurrence_count >= 1);
