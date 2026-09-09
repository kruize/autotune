/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.database.table.lm;

import com.autotune.analyzer.serviceObjects.BulkConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database entity to store Kruize optimizer bulk configuration.
 * Aligned with CreateExperiment structure for consistency.
 */
@Entity
@Table(name = "kruize_optimizer_bulk_config")
public class KruizeBulkConfigEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeBulkConfigEntry.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @Column(name = "config_name", columnDefinition = "VARCHAR(255)")
    private String configName;

    @Column(name = "cluster_name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String clusterName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datasources", columnDefinition = "jsonb", nullable = false)
    private JsonNode datasources;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "namespaces", columnDefinition = "jsonb", nullable = false)
    private JsonNode namespaces;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels", columnDefinition = "jsonb")
    private JsonNode labels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experiment_types", columnDefinition = "jsonb", nullable = false)
    private JsonNode experimentTypes;

    @Column(name = "metadata_profile", columnDefinition = "VARCHAR(255)")
    private String metadataProfile;

    @Column(name = "performance_profile", columnDefinition = "VARCHAR(255)", nullable = false)
    private String performanceProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trial_settings", columnDefinition = "jsonb")
    private JsonNode trialSettings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_settings", columnDefinition = "jsonb", nullable = false)
    private JsonNode recommendationSettings;

    @Column(name = "webhook_url", columnDefinition = "VARCHAR(500)")
    private String webhookUrl;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Default constructor
    public KruizeBulkConfigEntry() {
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public JsonNode getDatasources() {
        return datasources;
    }

    public void setDatasources(JsonNode datasources) {
        this.datasources = datasources;
    }

    public JsonNode getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(JsonNode namespaces) {
        this.namespaces = namespaces;
    }

    public JsonNode getLabels() {
        return labels;
    }

    public void setLabels(JsonNode labels) {
        this.labels = labels;
    }

    public JsonNode getExperimentTypes() {
        return experimentTypes;
    }

    public void setExperimentTypes(JsonNode experimentTypes) {
        this.experimentTypes = experimentTypes;
    }

    public String getMetadataProfile() {
        return metadataProfile;
    }

    public void setMetadataProfile(String metadataProfile) {
        this.metadataProfile = metadataProfile;
    }

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public JsonNode getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(JsonNode trialSettings) {
        this.trialSettings = trialSettings;
    }

    public JsonNode getRecommendationSettings() {
        return recommendationSettings;
    }

    public void setRecommendationSettings(JsonNode recommendationSettings) {
        this.recommendationSettings = recommendationSettings;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Convert this entity to a BulkConfig service object
     * @throws RuntimeException if required fields are null or conversion fails
     */
    public BulkConfig toBulkConfig() {
        BulkConfig config = new BulkConfig();
        config.setConfigName(this.configName);
        config.setClusterName(this.clusterName);

        // Required field: datasources
        if (this.datasources == null) {
            throw new IllegalStateException("Datasources cannot be null for config: " + configName);
        }
        try {
            List<String> datasourceList = objectMapper.convertValue(
                this.datasources,
                new TypeReference<List<String>>() {}
            );
            config.setDatasources(datasourceList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert datasources for config: " + configName, e);
        }
        
        // Required field: namespaces
        if (this.namespaces == null) {
            throw new IllegalStateException("Namespaces cannot be null for config: " + configName);
        }
        try {
            List<String> namespaceList = objectMapper.convertValue(
                this.namespaces,
                new TypeReference<List<String>>() {}
            );
            config.setNamespaces(namespaceList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert namespaces for config: " + configName, e);
        }
        
        // Optional field: labels
        if (this.labels != null) {
            try {
                Map<String, String> labelsMap = objectMapper.convertValue(
                    this.labels,
                    new TypeReference<Map<String, String>>() {}
                );
                config.setLabels(labelsMap);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert labels for config: " + configName, e);
            }
        }
        
        // Required field: experiment types
        if (this.experimentTypes == null) {
            throw new IllegalStateException("Experiment types cannot be null for config: " + configName);
        }
        try {
            List<String> experimentTypeList = objectMapper.convertValue(
                this.experimentTypes,
                new TypeReference<List<String>>() {}
            );
            config.setExperimentTypes(experimentTypeList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert experiment types for config: " + configName, e);
        }
        
        config.setMetadataProfile(this.metadataProfile);
        config.setPerformanceProfile(this.performanceProfile);

        // Optional field: trial settings
        if (this.trialSettings != null) {
            try {
                BulkConfig.TrialSettings trialSettings = objectMapper.convertValue(
                    this.trialSettings,
                    BulkConfig.TrialSettings.class
                );
                config.setTrialSettings(trialSettings);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert trial settings for config: " + configName, e);
            }
        }
        
        // Required field: recommendation settings
        if (this.recommendationSettings == null) {
            throw new IllegalStateException("Recommendation settings cannot be null for config: " + configName);
        }
        try {
            BulkConfig.RecommendationSettings recSettings = objectMapper.convertValue(
                this.recommendationSettings,
                BulkConfig.RecommendationSettings.class
            );
            config.setRecommendationSettings(recSettings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert recommendation settings for config: " + configName, e);
        }
        
        config.setWebhookUrl(this.webhookUrl);
        config.setEnabled(this.enabled);
        
        if (this.createdAt != null) {
            config.setCreatedAt(this.createdAt.toInstant());
        }
        if (this.updatedAt != null) {
            config.setUpdatedAt(this.updatedAt.toInstant());
        }
        
        return config;
    }

    /**
     * Create an entity from a BulkConfig service object
     * @param config the BulkConfig object to convert
     * @return KruizeBulkConfigEntry entity
     * @throws IllegalArgumentException if config is null or required fields are null
     * @throws RuntimeException if conversion fails
     */
    public static KruizeBulkConfigEntry fromBulkConfig(BulkConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("BulkConfig cannot be null");
        }

        KruizeBulkConfigEntry entry = new KruizeBulkConfigEntry();
        entry.setConfigName(config.getConfigName());
        entry.setClusterName(config.getClusterName());

        // Required field: datasources
        if (config.getDatasources() == null) {
            throw new IllegalArgumentException("Datasources cannot be null for config: " + config.getConfigName());
        }
        try {
            entry.setDatasources(objectMapper.valueToTree(config.getDatasources()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert datasources for config: " + config.getConfigName(), e);
        }
        
        // Required field: namespaces
        if (config.getNamespaces() == null) {
            throw new IllegalArgumentException("Namespaces cannot be null for config: " + config.getConfigName());
        }
        try {
            entry.setNamespaces(objectMapper.valueToTree(config.getNamespaces()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert namespaces for config: " + config.getConfigName(), e);
        }
        
        // Optional field: labels
        if (config.getLabels() != null) {
            try {
                entry.setLabels(objectMapper.valueToTree(config.getLabels()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert labels for config: " + config.getConfigName(), e);
            }
        }
        
        // Required field: experiment types
        if (config.getExperimentTypes() == null) {
            throw new IllegalArgumentException("Experiment types cannot be null for config: " + config.getConfigName());
        }
        try {
            entry.setExperimentTypes(objectMapper.valueToTree(config.getExperimentTypes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert experiment types for config: " + config.getConfigName(), e);
        }
        
        entry.setMetadataProfile(config.getMetadataProfile());
        entry.setPerformanceProfile(config.getPerformanceProfile());

        // Optional field: trial settings
        if (config.getTrialSettings() != null) {
            try {
                entry.setTrialSettings(objectMapper.valueToTree(config.getTrialSettings()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert trial settings for config: " + config.getConfigName(), e);
            }
        }
        
        // Required field: recommendation settings
        if (config.getRecommendationSettings() == null) {
            throw new IllegalArgumentException("Recommendation settings cannot be null for config: " + config.getConfigName());
        }
        try {
            entry.setRecommendationSettings(objectMapper.valueToTree(config.getRecommendationSettings()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert recommendation settings for config: " + config.getConfigName(), e);
        }
        
        entry.setWebhookUrl(config.getWebhookUrl());
        entry.setEnabled(config.getEnabled());
        
        if (config.getCreatedAt() != null) {
            entry.setCreatedAt(Timestamp.from(config.getCreatedAt()));
        }
        if (config.getUpdatedAt() != null) {
            entry.setUpdatedAt(Timestamp.from(config.getUpdatedAt()));
        }
        
        return entry;
    }

    @Override
    public String toString() {
        return "KruizeBulkConfigEntry{" +
                "configName='" + configName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", datasources=" + datasources +
                ", namespaces=" + namespaces +
                ", labels=" + labels +
                ", experimentTypes=" + experimentTypes +
                ", metadataProfile='" + metadataProfile + '\'' +
                ", performanceProfile='" + performanceProfile + '\'' +
                ", trialSettings=" + trialSettings +
                ", recommendationSettings=" + recommendationSettings +
                ", webhookUrl='" + webhookUrl + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
