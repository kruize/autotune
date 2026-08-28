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

import com.autotune.analyzer.serviceObjects.BulkConfig;
import com.autotune.analyzer.serviceObjects.BulkConfigUpdateRequest;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validation utility for Bulk Config API requests
 */
public class BulkConfigValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConfigValidation.class);

    // Valid values for recommendation settings
    private static final Set<String> VALID_TERMS = new HashSet<>(Arrays.asList(
            "short", "medium", "long"
    ));

    private static final Set<String> VALID_MODELS = new HashSet<>(Arrays.asList(
            "performance", "cost"
    ));

    private static final Set<String> VALID_EXPERIMENT_TYPES = new HashSet<>(Arrays.asList(
            "container", "namespace"
    ));

    // Regex pattern for scheduling format: number + unit (e.g., "24h", "30min", "7days")
    // Supports: h, hr, hrs, hour, hours, m, min, mins, minute, minutes, d, day, days
    private static final String SCHEDULING_PATTERN = "^\\d+\\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|d|day|days)$";

    /**
     * Validate a bulk config for creation
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

        // Validate performance_profile if provided
        if (bulkConfig.getPerformanceProfile() != null && !bulkConfig.getPerformanceProfile().trim().isEmpty()) {
            ValidationOutputData performanceProfileValidation = validatePerformanceProfileExists(bulkConfig.getPerformanceProfile());
            if (!performanceProfileValidation.isSuccess()) {
                return performanceProfileValidation;
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

        // Validate recommendation settings if provided
        if (updateRequest.getRecommendationSettings() != null) {
            ValidationOutputData settingsValidation = validateRecommendationSettings(updateRequest.getRecommendationSettings());
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
     */
    private static ValidationOutputData validateRecommendationSettings(BulkConfig.RecommendationSettings settings) {
        // Validate scheduling
        if (settings.getScheduling() == null) {
            return new ValidationOutputData(false,
                    "scheduling is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        ValidationOutputData schedulingValidation = validateScheduling(settings.getScheduling());
        if (!schedulingValidation.isSuccess()) {
            return schedulingValidation;
        }

        // Validate terms
        if (settings.getTerms() == null || settings.getTerms().isEmpty()) {
            return new ValidationOutputData(false,
                    "At least one term is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        for (String term : settings.getTerms()) {
            if (!VALID_TERMS.contains(term)) {
                return new ValidationOutputData(false,
                        "Invalid term: " + term + ". Valid values are: " + VALID_TERMS,
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        // Validate models
        if (settings.getModels() == null || settings.getModels().isEmpty()) {
            return new ValidationOutputData(false,
                    "At least one model is required in recommendation_settings",
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        for (String model : settings.getModels()) {
            if (!VALID_MODELS.contains(model)) {
                return new ValidationOutputData(false,
                        "Invalid model: " + model + ". Valid values are: " + VALID_MODELS,
                        HttpServletResponse.SC_BAD_REQUEST);
            }
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

        if (!scheduling.matches(SCHEDULING_PATTERN)) {
            return new ValidationOutputData(false,
                    "Invalid scheduling format: " + scheduling +
                            ". Expected format: number + unit (e.g., '24h', '30min', '7days'). " +
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
            List<com.autotune.database.table.lm.KruizeLMMetadataProfileEntry> entries =
                    dbService.loadMetadataProfileByName(metadataProfileName);
            
            if (entries == null || entries.isEmpty()) {
                return new ValidationOutputData(false,
                        "metadata_profile '" + metadataProfileName + "' does not exist. Please create it first using the createMetadataProfile API.",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.error("Error validating metadata_profile '{}': {}", metadataProfileName, e.getMessage());
            return new ValidationOutputData(false,
                    "Error validating metadata_profile: " + e.getMessage(),
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
            List<com.autotune.database.table.KruizePerformanceProfileEntry> entries =
                    dbService.loadPerformanceProfileByName(performanceProfileName);
            
            if (entries == null || entries.isEmpty()) {
                return new ValidationOutputData(false,
                        "performance_profile '" + performanceProfileName + "' does not exist. Please create it first using the createPerformanceProfile API.",
                        HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            LOGGER.error("Error validating performance_profile '{}': {}", performanceProfileName, e.getMessage());
            return new ValidationOutputData(false,
                    "Error validating performance_profile: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return new ValidationOutputData(true, null, HttpServletResponse.SC_OK);
    }

    /**
     * Validate datasource connection and reachability
     * This method follows the same pattern as BulkServiceValidation.validateDatasourceConnection()
     *
     * @param datasourceName The name of the datasource to validate
     * @return Empty string if valid, error message otherwise
     */
    public static String validateDatasourceConnection(String datasourceName) {
        String errorMessage = "";
        try {
            DataSourceInfo dataSourceInfo = null;
            try {
                dataSourceInfo = new ExperimentDBService().loadDataSourceFromDBByName(datasourceName);
            } catch (Exception e) {
                errorMessage = String.format(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.LOAD_DATASOURCE_FROM_DB_ERROR, datasourceName, e.getMessage());
                LOGGER.error(errorMessage);
                return errorMessage;
            }
            LOGGER.info(KruizeConstants.DataSourceConstants.DataSourceInfoMsgs.VERIFYING_DATASOURCE_REACHABILITY, datasourceName);
            DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(KruizeConstants.SupportedDatasources.PROMETHEUS);
            if (dataSourceInfo == null || op.isServiceable(dataSourceInfo) == CommonUtils.DatasourceReachabilityStatus.NOT_REACHABLE) {
                errorMessage = KruizeConstants.DataSourceConstants.DataSourceErrorMsgs.DATASOURCE_NOT_SERVICEABLE;
                LOGGER.error(errorMessage);
            }
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            LOGGER.error(errorMessage);
        }
        return errorMessage;
    }
}
