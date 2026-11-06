ALTER TABLE kruize_datasources
ADD COLUMN IF NOT EXISTS clusters JSONB;

-- Add comment for documentation
COMMENT ON COLUMN kruize_datasources.clusters IS 'JSON array of cluster names associated with this datasource, e.g. ["cluster-1","cluster-2"]';

-- Note: No default value is set to maintain backward compatibility
-- Existing datasources will have NULL clusters, which is handled gracefully in the application code
