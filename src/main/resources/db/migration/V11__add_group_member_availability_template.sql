ALTER TABLE group_member
    ADD COLUMN availability_template_id UUID REFERENCES availability_template(id) ON DELETE SET NULL;

CREATE INDEX idx_group_member_availability_template
    ON group_member (availability_template_id);
