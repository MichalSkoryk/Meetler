UPDATE subscription_plan
SET
    max_owned_groups = 5,
    max_availability_templates = 5,
    updated_at = NOW()
WHERE code = 'FREE';

UPDATE subscription_plan
SET
    max_owned_groups = 15,
    max_availability_templates = 15,
    updated_at = NOW()
WHERE code = 'PLUS';
