/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.serviceObjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service object representing an Optimiser Bulk Configuration.
 * Aligned with CreateExperiment API structure for consistency.
 */
public class BulkConfig {
    @JsonProperty("config_name")
    private String configName;

    @JsonProperty("cluster_name")
    private String clusterName;

    @JsonProperty("datasources")
    private List<String> datasources = new ArrayList<>();

    @JsonProperty("namespaces")
    private List<String> namespaces = new ArrayList<>();

    @JsonProperty("labels")
    private Map<String, String> labels = new HashMap<>();

    @JsonProperty("experiment_types")
    private List<String> experimentTypes = new ArrayList<>();

    @JsonProperty("metadata_profile")
    private String metadataProfile;

    @JsonProperty("performance_profile")
    private String performanceProfile;

    @JsonProperty("trial_settings")
    private TrialSettings trialSettings;

    @JsonProperty("recommendation_settings")
    private RecommendationSettings recommendationSettings;

    @JsonProperty("webhook_url")
    private String webhookUrl;

    @JsonProperty("enabled")
    private Boolean enabled = true;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public BulkConfig() {
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

    public List<String> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<String> datasources) {
        if (null == datasources)
            datasources = new ArrayList<>();
        this.datasources = datasources;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        if (null == namespaces)
            namespaces = new ArrayList<>();
        this.namespaces = namespaces;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        if (null == labels)
            labels = new HashMap<>();
        this.labels = labels;
    }

    public List<String> getExperimentTypes() {
        return experimentTypes;
    }

    public void setExperimentTypes(List<String> experimentTypes) {
        if (null == experimentTypes)
            experimentTypes = new ArrayList<>();
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

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(TrialSettings trialSettings) {
        this.trialSettings = trialSettings;
    }

    public RecommendationSettings getRecommendationSettings() {
        return recommendationSettings;
    }

    public void setRecommendationSettings(RecommendationSettings recommendationSettings) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Trial settings for the optimiser bulk config (matches CreateExperiment structure)
     */
    public static class TrialSettings {
        @JsonProperty("measurement_duration")
        private String measurementDurationMinutes;

        public TrialSettings() {
        }

        public TrialSettings(String measurementDurationMinutes) {
            this.measurementDurationMinutes = measurementDurationMinutes;
        }

        public String getMeasurementDurationMinutes() {
            return measurementDurationMinutes;
        }

        public void setMeasurementDurationMinutes(String measurementDurationMinutes) {
            this.measurementDurationMinutes = measurementDurationMinutes;
        }
    }

    /**
     * Recommendation settings for the optimiser bulk config
     */
    public static class RecommendationSettings {
        @JsonProperty("scheduling")
        private String scheduling;

        @JsonProperty("terms")
        private List<String> terms = new ArrayList<>();

        @JsonProperty("models")
        private List<String> models = new ArrayList<>();

        public RecommendationSettings() {
        }

        public RecommendationSettings(String scheduling, List<String> terms, List<String> models) {
            this.scheduling = scheduling;
            this.terms = terms;
            this.models = models;
        }

        public String getScheduling() {
            return scheduling;
        }

        public void setScheduling(String scheduling) {
            this.scheduling = scheduling;
        }

        public List<String> getTerms() {
            return terms;
        }

        public void setTerms(List<String> terms) {
            if (null == terms)
                terms = new ArrayList<>();
            this.terms = terms;
        }

        public List<String> getModels() {
            return models;
        }

        public void setModels(List<String> models) {
            if (null == models)
                models = new ArrayList<>();
            this.models = models;
        }
    }
}
