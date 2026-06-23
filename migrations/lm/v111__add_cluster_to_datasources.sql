ALTER TABLE kruize_datasources
ADD COLUMN IF NOT EXISTS clusters TEXT;

-- Add comment for documentation
COMMENT ON COLUMN kruize_datasources.clusters IS 'Comma-separated list of cluster names associated with this datasource';

-- Note: No default value is set to maintain backward compatibility
-- Existing datasources will have NULL clusters, which is handled gracefully in the application code
