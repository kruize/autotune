alter table kruize_lm_experiments add column if not exists metadata_id bigint references kruize_dsmetadata(id);
