ALTER TABLE calendar
    DROP CONSTRAINT IF EXISTS calendar_provider_check;

ALTER TABLE calendar
    ADD CONSTRAINT calendar_provider_check
        CHECK (provider IN ('GOOGLE', 'TEAMS', 'INTERNAL'));
