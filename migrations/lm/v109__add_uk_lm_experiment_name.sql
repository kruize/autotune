DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_lm_experiment_name'
    ) THEN
        ALTER TABLE kruize_lm_experiments ADD CONSTRAINT UK_lm_experiment_name UNIQUE (experiment_name);
    END IF;
END
$$;
