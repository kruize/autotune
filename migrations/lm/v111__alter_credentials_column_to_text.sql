ALTER TABLE kruize_authentication
ALTER COLUMN credentials TYPE TEXT
USING CASE
    WHEN jsonb_typeof(credentials) = 'string' THEN credentials #>> '{}'
    ELSE credentials::TEXT
END;
