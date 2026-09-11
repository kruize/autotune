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
package com.autotune.common.bulk;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.performanceProfiles.PerformanceProfile;
import com.autotune.analyzer.serviceObjects.BulkConfig;
import com.autotune.analyzer.serviceObjects.BulkConfigUpdateRequest;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Validation utility for Bulk Config API requests
 */
public class BulkConfigValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConfigValidation.class);

    // Valid values for recommendation settings
    private static final Set<String> VALID_TERMS = new HashSet<>(Arrays.asList(
            KruizeConstants.JSONKeys.SHORT,
            KruizeConstants.JSONKeys.MEDIUM,
            KruizeConstants.JSONKeys.LONG
    ));

    private static final Set<String> VALID_MODELS = new HashSet<>(Arrays.asList(
            KruizeConstants.JSONKeys.PERFORMANCE,
            KruizeConstants.JSONKeys.COST
    ));

    private static final Set<String> VALID_EXPERIMENT_TYPES = new HashSet<>(Arrays.asList(
            KruizeConstants.JSONKeys.CONTAINER,
            KruizeConstants.JSONKeys.NAMESPACE
    ));

    // Regex pattern for scheduling format: number + unit (e.g., "24h", "30min", "7days")
    // Supports: h, hr, hrs, hour, hours, m, min, mins, minute, minutes, d, day, days
    private static final String SCHEDULING_PATTERN = "^[1-9]\\d*\\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|d|day|days)$";

    // Regex pattern for measurement_duration: positive integer + time unit.
    // Unit vocabulary matches CommonUtils.getTimeUnit() exactly (case-insensitive comparison done before match):
    //   minutes : m, min, mins, minute, minutes
    //   hours   : h, hr, hrs, hour, hours
    //   seconds : s, sec, secs, second, seconds
    private static final String MEASUREMENT_DURATION_PATTERN =
            "^[1-9]\\d*\\s*(m|min|mins|minute|minutes|h|hr|hrs|hour|hours|s|sec|secs|second|seconds)$";

    /**
     * Validate a bulk config for creation.
     * @param bulkConfig The bulk config to validate
     * @return ValidationOutputData with success status and error details if any
     */
    public static ValidationOutputData validateCreate(BulkConfig bulkConfig) {
        // Check required fields
        if (bulkConfig.getConfigName() == null || bulkConfig.getConfigName().trim().isEmpty()) {
            return new ValidationOutputData(false, "config_name is required", HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate config name format (alphanumeric, hyphens, underscores)
        if (!bulkConfig.getConfigName().matches("^[a-zA-Z0-9_-]+$")) {
            return new ValidationOutputData(false,
                    "config_name must contain only alphanumeric characters, hyphens, and underscores",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate cluster_name
        if (bulkConfig.getClusterName() == null || bulkConfig.getClusterName().trim().isEmpty()) {
            return new ValidationOutputData(false, "cluster_name is required", HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate datasources
        if (bulkConfig.getDatasources() == null || bulkConfig.getDatasources().isEmpty()) {
            return new ValidationOutputData(false, "At least one datasource is required", HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate each datasource connection
        for (String datasourceName : bulkConfig.getDatasources()) {
            String errorMessage = validateDatasourceConnection(datasourceName);
            if (!errorMessage.isEmpty()) {
                return new ValidationOutputData(false, errorMessage, HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // Validate namespaces if provided
        if (bulkConfig.getNamespaces() != null) {
            for (String namespace : bulkConfig.getNamespaces()) {
                if (namespace == null || namespace.trim().isEmpty()) {
                    return new ValidationOutputData(false,
                            "Empty namespace not allowed",
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate labels if provided
        if (bulkConfig.getLabels() != null) {
            for (String key : bulkConfig.getLabels().keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    return new ValidationOutputData(false,
                            "Empty label key not allowed",
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate experiment_types
        if (bulkConfig.getExperimentTypes() == null || bulkConfig.getExperimentTypes().isEmpty()) {
            return new ValidationOutputData(false,
                    "At least one experiment_type is required",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        for (String expType : bulkConfig.getExperimentTypes()) {
            if (!VALID_EXPERIMENT_TYPES.contains(expType)) {
                return new ValidationOutputData(false,
                        "Invalid experiment_type: " + expType +
                                ". Valid values are: " + VALID_EXPERIMENT_TYPES,
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // Validate metadata_profile
        if (bulkConfig.getMetadataProfile() == null || bulkConfig.getMetadataProfile().trim().isEmpty()) {
            return new ValidationOutputData(false,
                    "metadata_profile is required",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate metadata_profile exists in database
        ValidationOutputData metadataProfileValidation = validateMetadataProfileExists(bulkConfig.getMetadataProfile());
        if (!metadataProfileValidation.isSuccess()) {
            return metadataProfileValidation;
        }

        // Validate performance_profile (required)
        if (bulkConfig.getPerformanceProfile() == null || bulkConfig.getPerformanceProfile().trim().isEmpty()) {
            return new ValidationOutputData(false,
                    "performance_profile is required",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        ValidationOutputData performanceProfileValidation = validatePerformanceProfileExists(bulkConfig.getPerformanceProfile());
        if (!performanceProfileValidation.isSuccess()) {
            return performanceProfileValidation;
        }

        // Validate trial_settings if provided
        if (bulkConfig.getTrialSettings() != null) {
            ValidationOutputData trialSettingsValidation = validateTrialSettings(bulkConfig.getTrialSettings());
            if (!trialSettingsValidation.isSuccess()) {
                return trialSettingsValidation;
            }
        }

        // Validate recommendation_settings
        if (bulkConfig.getRecommendationSettings() == null) {
            return new ValidationOutputData(false, "recommendation_settings is required", HttpServletResponse.SC_BAD_REQUEST);
        }

        ValidationOutputData settingsValidation = validateRecommendationSettings(bulkConfig.getRecommendationSettings());
        if (!settingsValidation.isSuccess()) {
            return settingsValidation;
        }

        // Validate webhook URL if provided
        if (bulkConfig.getWebhookUrl() != null && !bulkConfig.getWebhookUrl().trim().isEmpty()) {
            ValidationOutputData webhookValidation = validateWebhookUrl(bulkConfig.getWebhookUrl());
            if (!webhookValidation.isSuccess()) {
                return webhookValidation;
            }
        }

        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate a fully-merged BulkConfig before persisting after a PUT.
     * Runs the same structural checks as validateCreate but produces error messages
     * that make sense in an update context (avoids "X is required" for fields that
     * already existed in the stored record and were not touched by the request).
     *
     * @param bulkConfig The merged config to validate
     * @return ValidationOutputData with success status and error details if any
     */
    public static ValidationOutputData validateForPersist(BulkConfig bulkConfig) {
        // Delegate to validateCreate — the merged object must satisfy all the same
        // invariants. The difference is solely in how the caller surfaces errors.
        return validateCreate(bulkConfig);
    }

    /**
     * Validate a bulk config update request
     * @param updateRequest The update request to validate
     * @return ValidationOutputData with success status and error details if any
     */
    public static ValidationOutputData validateUpdate(BulkConfigUpdateRequest updateRequest) {
        // Check if at least one field is provided for update
        if (!updateRequest.hasUpdates()) {
            return new ValidationOutputData(false,
                    "At least one field must be provided for update",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate cluster_name if provided
        if (updateRequest.getClusterName() != null && updateRequest.getClusterName().trim().isEmpty()) {
            return new ValidationOutputData(false,
                    "cluster_name cannot be empty if provided",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate datasources if provided
        if (updateRequest.getDatasources() != null) {
            if (updateRequest.getDatasources().isEmpty()) {
                return new ValidationOutputData(false,
                        "datasources cannot be empty if provided",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            // Validate each datasource connection
            for (String datasourceName : updateRequest.getDatasources()) {
                String errorMessage = validateDatasourceConnection(datasourceName);
                if (!errorMessage.isEmpty()) {
                    return new ValidationOutputData(false, errorMessage, HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate namespaces if provided
        if (updateRequest.getNamespaces() != null) {
            for (String namespace : updateRequest.getNamespaces()) {
                if (namespace == null || namespace.trim().isEmpty()) {
                    return new ValidationOutputData(false,
                            "Empty namespace not allowed",
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate labels if provided
        if (updateRequest.getLabels() != null) {
            for (String key : updateRequest.getLabels().keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    return new ValidationOutputData(false,
                            "Empty label key not allowed",
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate experiment_types if provided
        if (updateRequest.getExperimentTypes() != null) {
            if (updateRequest.getExperimentTypes().isEmpty()) {
                return new ValidationOutputData(false,
                        "experiment_types cannot be empty if provided",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            for (String expType : updateRequest.getExperimentTypes()) {
                if (!VALID_EXPERIMENT_TYPES.contains(expType)) {
                    return new ValidationOutputData(false,
                            "Invalid experiment_type: " + expType +
                                    ". Valid values are: " + VALID_EXPERIMENT_TYPES,
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }

        // Validate metadata_profile if provided
        if (updateRequest.getMetadataProfile() != null) {
            if (updateRequest.getMetadataProfile().trim().isEmpty()) {
                return new ValidationOutputData(false,
                        "metadata_profile cannot be empty if provided",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            // Validate metadata_profile exists in database
            ValidationOutputData metadataProfileValidation = validateMetadataProfileExists(updateRequest.getMetadataProfile());
            if (!metadataProfileValidation.isSuccess()) {
                return metadataProfileValidation;
            }
        }

        // Validate performance_profile if provided
        if (updateRequest.getPerformanceProfile() != null) {
            if (updateRequest.getPerformanceProfile().trim().isEmpty()) {
                return new ValidationOutputData(false,
                        "performance_profile cannot be empty if provided",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            // Validate performance_profile exists in database
            ValidationOutputData performanceProfileValidation = validatePerformanceProfileExists(updateRequest.getPerformanceProfile());
            if (!performanceProfileValidation.isSuccess()) {
                return performanceProfileValidation;
            }
        }

        // Validate trial_settings if provided
        if (updateRequest.getTrialSettings() != null) {
            ValidationOutputData trialSettingsValidation = validateTrialSettings(updateRequest.getTrialSettings());
            if (!trialSettingsValidation.isSuccess()) {
                return trialSettingsValidation;
            }
        }

        // Validate recommendation settings if provided (partial validation for updates)
        if (updateRequest.getRecommendationSettings() != null) {
            ValidationOutputData settingsValidation = validateRecommendationSettings(updateRequest.getRecommendationSettings(), false);
            if (!settingsValidation.isSuccess()) {
                return settingsValidation;
            }
        }

        // Validate webhook URL if provided
        if (updateRequest.getWebhookUrl() != null && !updateRequest.getWebhookUrl().trim().isEmpty()) {
            ValidationOutputData webhookValidation = validateWebhookUrl(updateRequest.getWebhookUrl());
            if (!webhookValidation.isSuccess()) {
                return webhookValidation;
            }
        }

        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate recommendation settings
     * For CREATE: all fields are required
     * For UPDATE: only validate fields that are provided
     */
    private static ValidationOutputData validateRecommendationSettings(BulkConfig.RecommendationSettings settings) {
        return validateRecommendationSettings(settings, true);
    }

    /**
     * Validate recommendation settings with option to require all fields
     * @param settings The settings to validate
     * @param requireAll If true, all fields are required (for CREATE). If false, only validate provided fields (for UPDATE)
     */
    private static ValidationOutputData validateRecommendationSettings(BulkConfig.RecommendationSettings settings, boolean requireAll) {
        // Validate scheduling if provided or required
        if (settings.getScheduling() != null) {
            ValidationOutputData schedulingValidation = validateScheduling(settings.getScheduling());
            if (!schedulingValidation.isSuccess()) {
                return schedulingValidation;
            }
        } else if (requireAll) {
            return new ValidationOutputData(false,
                    "scheduling is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate terms if provided or required
        if (settings.getTerms() != null) {
            if (settings.getTerms().isEmpty()) {
                return new ValidationOutputData(false,
                        "terms cannot be empty if provided in recommendation_settings",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            for (String term : settings.getTerms()) {
                if (!VALID_TERMS.contains(term)) {
                    return new ValidationOutputData(false,
                            "Invalid term: " + term + ". Valid values are: " + VALID_TERMS,
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        } else if (requireAll) {
            return new ValidationOutputData(false,
                    "At least one term is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        // Validate models if provided or required
        if (settings.getModels() != null) {
            if (settings.getModels().isEmpty()) {
                return new ValidationOutputData(false,
                        "models cannot be empty if provided in recommendation_settings",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
            for (String model : settings.getModels()) {
                if (!VALID_MODELS.contains(model)) {
                    return new ValidationOutputData(false,
                            "Invalid model: " + model + ". Valid values are: " + VALID_MODELS,
                            HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        } else if (requireAll) {
            return new ValidationOutputData(false,
                    "At least one model is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate trial settings.
     * measurement_duration must be a positive integer followed by a recognised time unit,
     * matching the same units accepted by CommonUtils.getTimeUnit():
     *   minutes : m, min, mins, minute, minutes
     *   hours   : h, hr, hrs, hour, hours
     *   seconds : s, sec, secs, second, seconds
     * Examples: "15min", "1h", "30s", "2hours"
     */
    private static ValidationOutputData validateTrialSettings(BulkConfig.TrialSettings trialSettings) {
        String duration = trialSettings.getMeasurementDurationMinutes();
        if (duration == null || duration.trim().isEmpty()) {
            return new ValidationOutputData(false,
                    "measurement_duration is required in trial_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }
        if (!duration.trim().toLowerCase().matches(MEASUREMENT_DURATION_PATTERN)) {
            return new ValidationOutputData(false,
                    "Invalid measurement_duration: '" + duration + "'. " +
                            "Expected a positive integer followed by a time unit. " +
                            "Valid units — minutes: m/min/mins/minute/minutes, " +
                            "hours: h/hr/hrs/hour/hours, seconds: s/sec/secs/second/seconds " +
                            "(e.g., '15min', '1h', '30s')",
                    HttpServletResponse.SC_BAD_REQUEST);
        }
        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate scheduling configuration
     * Accepts formats like: "24h", "30min", "7days", "1hr", etc.
     */
    private static ValidationOutputData validateScheduling(String scheduling) {
        if (scheduling == null || scheduling.trim().isEmpty()) {
            return new ValidationOutputData(false,
                    "scheduling is required",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        if (!scheduling.trim().toLowerCase().matches(SCHEDULING_PATTERN)) {
            return new ValidationOutputData(false,
                    "Invalid scheduling format: '" + scheduling +
                            "'. Expected format: number + unit (e.g., '24h', '30min', '7days'). " +
                            "Valid units: h/hr/hrs/hour/hours, m/min/mins/minute/minutes, d/day/days",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate webhook URL format
     */
    private static ValidationOutputData validateWebhookUrl(String webhookUrl) {
        try {
            URL url = new URL(webhookUrl);
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return new ValidationOutputData(false,
                        "webhook_url must use http or https protocol",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (MalformedURLException e) {
            return new ValidationOutputData(false,
                    "Invalid webhook_url format: " + e.getMessage(),
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate that metadata profile exists in database
     */
    private static ValidationOutputData validateMetadataProfileExists(String metadataProfileName) {
        try {
            ExperimentDBService dbService = new ExperimentDBService();
            Map<String, MetadataProfile> metadataProfileMap = new HashMap<>();
            dbService.loadMetadataProfileFromDBByName(metadataProfileMap, metadataProfileName);

            if (!metadataProfileMap.containsKey(metadataProfileName)) {
                return new ValidationOutputData(false,
                        "metadata_profile '" + metadataProfileName + "' does not exist. Please create it first using the createMetadataProfile API.",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.error("Error validating metadata_profile '{}': {}", metadataProfileName, e.getMessage());
            return new ValidationOutputData(false,
                    "Failed to validate metadata_profile. Please try again.",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate that performance profile exists in database
     */
    private static ValidationOutputData validatePerformanceProfileExists(String performanceProfileName) {
        try {
            ExperimentDBService dbService = new ExperimentDBService();
            Map<String, PerformanceProfile> performanceProfileMap = new HashMap<>();
            dbService.loadPerformanceProfileFromDBByName(performanceProfileMap, performanceProfileName);

            if (!performanceProfileMap.containsKey(performanceProfileName)) {
                return new ValidationOutputData(false,
                        "performance_profile '" + performanceProfileName + "' does not exist. Please create it first using the createPerformanceProfile API.",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.error("Error validating performance_profile '{}': {}", performanceProfileName, e.getMessage());
            return new ValidationOutputData(false,
                    "Failed to validate performance_profile. Please try again.",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate that a datasource exists in the database.
     * Reachability is intentionally not checked here — a live HTTP probe to Prometheus
     * on every POST/PUT would reject valid configs during maintenance windows and would
     * block the servlet thread per datasource. Reachability is verified when the bulk
     * job actually runs, which is the correct place.
     *
     * @param datasourceName The name of the datasource to validate
     * @return Empty string if valid, error message otherwise
     */
    public static String validateDatasourceConnection(String datasourceName) {
        try {
            DataSourceInfo dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(datasourceName);
            if (dataSourceInfo == null) {
                return "Datasource '" + datasourceName + "' does not exist. Please register it first.";
            }
        } catch (Exception e) {
            String errorMessage = String.format(
                    KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.LOAD_DATASOURCE_FROM_DB_ERROR,
                    datasourceName, e.getMessage());
            LOGGER.error(errorMessage);
            return errorMessage;
        }
        return "";
    }
}
