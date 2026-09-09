CREATE TABLE IF NOT EXISTS kruize_optimizer_bulk_config (
    config_name VARCHAR(255) PRIMARY KEY,
    cluster_name VARCHAR(255) NOT NULL,
    datasources JSONB NOT NULL,
    namespaces JSONB NOT NULL,
    labels JSONB,
    experiment_types JSONB NOT NULL,
    metadata_profile VARCHAR(255),
    performance_profile VARCHAR(255) NOT NULL,
    trial_settings JSONB,
    recommendation_settings JSONB NOT NULL,
    webhook_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
