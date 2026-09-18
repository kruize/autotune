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
package com.autotune.analyzer.services;

import com.autotune.analyzer.serviceObjects.BulkConfig;
import com.autotune.analyzer.serviceObjects.BulkConfigUpdateRequest;
import com.autotune.common.bulk.BulkConfigValidation;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.database.dao.ExperimentDAO;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.table.lm.KruizeBulkConfigEntry;
import com.autotune.utils.GenericRestApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API service for Bulk Config management
 * Provides CRUD operations for bulk configs and webhook notifications
 */
@WebServlet(asyncSupported = true)
public class BulkConfigService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkConfigService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private ExperimentDAO experimentDAO;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        experimentDAO = new ExperimentDAOImpl();
    }

    /**
     * GET /bulkConfigs - List all bulk configs or get a specific config
     * Query parameters:
     * - config_name: (optional) Get specific config by name
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String configName = req.getParameter("config_name");

            if (configName != null && !configName.trim().isEmpty()) {
                // Get specific config
                handleGetConfig(configName, resp, out);
            } else {
                // List all configs
                handleListConfigs(resp, out);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing GET request: {}", e.getMessage(), e);
            sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    /**
     * POST /bulkConfigs - Create a new bulk config
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // Parse request body
            String requestBody = req.getReader().lines().collect(Collectors.joining());
            LOGGER.info("Received bulk config creation request: {}", requestBody);

            BulkConfig bulkConfig = objectMapper.readValue(requestBody, BulkConfig.class);

            // Validate the config
            ValidationOutputData validation = BulkConfigValidation.validateCreate(bulkConfig);
            if (!validation.isSuccess()) {
                sendErrorResponse(resp, out, validation.getErrorCode(), validation.getMessage());
                return;
            }

            // Check if config already exists
            KruizeBulkConfigEntry existingConfig = experimentDAO.loadBulkConfigByName(bulkConfig.getConfigName());
            if (existingConfig != null) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_CONFLICT,
                        "Bulk config with name '" + bulkConfig.getConfigName() + "' already exists");
                return;
            }

            // Stamp creation and update timestamps before persisting
            Instant now = Instant.now();
            bulkConfig.setCreatedAt(now);
            bulkConfig.setUpdatedAt(now);

            // Convert to database entity and save
            KruizeBulkConfigEntry configEntry = KruizeBulkConfigEntry.fromBulkConfig(bulkConfig);
            LOGGER.info("Saving bulk config '{}' to database", bulkConfig.getConfigName());

            ValidationOutputData saveResult = experimentDAO.addBulkConfigToDB(configEntry);

            if (!saveResult.isSuccess()) {
                LOGGER.error("Failed to save bulk config '{}' to database: {}",
                        bulkConfig.getConfigName(), saveResult.getMessage());
                sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to create bulk config: " + saveResult.getMessage());
                return;
            }

            LOGGER.info("Successfully saved bulk config '{}' to database", bulkConfig.getConfigName());

            // Return success response
            resp.setStatus(HttpServletResponse.SC_CREATED);

            out.write(objectMapper.writeValueAsString(bulkConfig));

            LOGGER.info("Bulk config '{}' created successfully with scheduling: {}",
                    bulkConfig.getConfigName(),
                    bulkConfig.getRecommendationSettings() != null
                            ? bulkConfig.getRecommendationSettings().getScheduling()
                            : null);

        } catch (Exception e) {
            LOGGER.error("Error creating bulk config: {}", e.getMessage(), e);
            sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    /**
     * PUT /bulkConfigs?config_name=<name> - Update an existing bulk config
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String configName = req.getParameter("config_name");
            if (configName == null || configName.trim().isEmpty()) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_BAD_REQUEST,
                        "config_name query parameter is required");
                return;
            }

            // Parse request body
            String requestBody = req.getReader().lines().collect(Collectors.joining());
            BulkConfigUpdateRequest updateRequest = objectMapper.readValue(requestBody, BulkConfigUpdateRequest.class);

            // Validate the update request
            ValidationOutputData validation = BulkConfigValidation.validateUpdate(updateRequest);
            if (!validation.isSuccess()) {
                sendErrorResponse(resp, out, validation.getErrorCode(), validation.getMessage());
                return;
            }

            // Load existing config
            KruizeBulkConfigEntry existingEntry = experimentDAO.loadBulkConfigByName(configName);

            if (existingEntry == null) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_NOT_FOUND,
                        "Bulk config not found: " + configName);
                return;
            }

            // Convert existing entry to BulkConfig
            BulkConfig existingConfig = existingEntry.toBulkConfig();

            // Apply updates
            if (updateRequest.getClusterName() != null) {
                existingConfig.setClusterName(updateRequest.getClusterName());
            }
            if (updateRequest.getDatasources() != null) {
                existingConfig.setDatasources(updateRequest.getDatasources());
            }
            if (updateRequest.getNamespaces() != null) {
                existingConfig.setNamespaces(updateRequest.getNamespaces());
            }
            if (updateRequest.getLabels() != null) {
                existingConfig.setLabels(updateRequest.getLabels());
            }
            if (updateRequest.getExperimentTypes() != null) {
                existingConfig.setExperimentTypes(updateRequest.getExperimentTypes());
            }
            if (updateRequest.getMetadataProfile() != null) {
                existingConfig.setMetadataProfile(updateRequest.getMetadataProfile());
            }
            if (updateRequest.getPerformanceProfile() != null) {
                existingConfig.setPerformanceProfile(updateRequest.getPerformanceProfile());
            }
            // Handle trial_settings with granular updates
            if (updateRequest.getTrialSettings() != null) {
                BulkConfig.TrialSettings existingTrialSettings = existingConfig.getTrialSettings();
                BulkConfig.TrialSettings updateTrialSettings = updateRequest.getTrialSettings();
                
                // If existing trial_settings is null, use the update as-is
                if (existingTrialSettings == null) {
                    existingConfig.setTrialSettings(updateTrialSettings);
                } else {
                    // Only update the fields that are provided
                    if (updateTrialSettings.getMeasurementDurationMinutes() != null) {
                        existingTrialSettings.setMeasurementDurationMinutes(updateTrialSettings.getMeasurementDurationMinutes());
                    }
                    existingConfig.setTrialSettings(existingTrialSettings);
                }
            }
            // Handle recommendation_settings with granular updates
            if (updateRequest.getRecommendationSettings() != null) {
                BulkConfig.RecommendationSettings existingSettings = existingConfig.getRecommendationSettings();
                BulkConfig.RecommendationSettings updateSettings = updateRequest.getRecommendationSettings();

                // If there are no existing settings, use the incoming object as-is
                if (existingSettings == null) {
                    existingConfig.setRecommendationSettings(updateSettings);
                } else {
                    // Only overwrite the individual fields that were supplied
                    if (updateSettings.getScheduling() != null) {
                        existingSettings.setScheduling(updateSettings.getScheduling());
                    }
                    if (updateSettings.getTerms() != null && !updateSettings.getTerms().isEmpty()) {
                        existingSettings.setTerms(updateSettings.getTerms());
                    }
                    if (updateSettings.getModels() != null && !updateSettings.getModels().isEmpty()) {
                        existingSettings.setModels(updateSettings.getModels());
                    }
                    existingConfig.setRecommendationSettings(existingSettings);
                }
            }
            if (updateRequest.getEnabled() != null) {
                existingConfig.setEnabled(updateRequest.getEnabled());
            }
            if (updateRequest.getWebhookUrl() != null) {
                existingConfig.setWebhookUrl(updateRequest.getWebhookUrl());
            }

            // Re-validate the fully-merged config before persisting
            ValidationOutputData fullValidation = BulkConfigValidation.validateForPersist(existingConfig);
            if (!fullValidation.isSuccess()) {
                sendErrorResponse(resp, out, fullValidation.getErrorCode(), fullValidation.getMessage());
                return;
            }

            // Stamp the update timestamp before persisting
            existingConfig.setUpdatedAt(Instant.now());

            // Convert back to database entity and update
            KruizeBulkConfigEntry updatedEntry = KruizeBulkConfigEntry.fromBulkConfig(existingConfig);

            ValidationOutputData updateResult = experimentDAO.updateBulkConfigToDB(updatedEntry);
            if (!updateResult.isSuccess()) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to update bulk config: " + updateResult.getMessage());
                return;
            }

            // Trigger webhook if URL is configured
            if (existingConfig.getWebhookUrl() != null && !existingConfig.getWebhookUrl().trim().isEmpty()) {
                triggerWebhook(existingConfig);
            }

            // Return updated config
            resp.setStatus(HttpServletResponse.SC_OK);

            out.write(objectMapper.writeValueAsString(existingConfig));
            LOGGER.info("Updated bulk config: {}", configName);

        } catch (Exception e) {
            LOGGER.error("Error updating bulk config: {}", e.getMessage(), e);
            sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    /**
     * DELETE /bulkConfigs?config_name=<name> - Delete a bulk config
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String configName = req.getParameter("config_name");
            if (configName == null || configName.trim().isEmpty()) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_BAD_REQUEST,
                        "config_name query parameter is required");
                return;
            }

            // Check if config exists
            KruizeBulkConfigEntry existingConfig = experimentDAO.loadBulkConfigByName(configName);
            if (existingConfig == null) {
                sendErrorResponse(resp, out, HttpServletResponse.SC_NOT_FOUND,
                        "Bulk config not found: " + configName);
                return;
            }

            // Delete the config
            ValidationOutputData deleteResult = experimentDAO.deleteBulkConfigByName(configName);
            if (!deleteResult.isSuccess()) {
                sendErrorResponse(resp, out, deleteResult.getErrorCode(),
                        "Failed to delete bulk config: " + deleteResult.getMessage());
                return;
            }

            // Return success response
            resp.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"message\":\"Bulk config deleted successfully\",\"config_name\":\"" + configName + "\"}");

            LOGGER.info("Deleted bulk config: {}", configName);

        } catch (Exception e) {
            LOGGER.error("Error deleting bulk config: {}", e.getMessage(), e);
            sendErrorResponse(resp, out, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    /**
     * Handle GET request for a specific config
     */
    private void handleGetConfig(String configName, HttpServletResponse resp, PrintWriter out) throws Exception {
        KruizeBulkConfigEntry configEntry = experimentDAO.loadBulkConfigByName(configName);

        if (configEntry == null) {
            sendErrorResponse(resp, out, HttpServletResponse.SC_NOT_FOUND,
                    "Bulk config not found: " + configName);
            return;
        }

        BulkConfig config = configEntry.toBulkConfig();
        resp.setStatus(HttpServletResponse.SC_OK);
        out.write(objectMapper.writeValueAsString(config));
    }

    /**
     * Handle GET request to list all configs
     */
    private void handleListConfigs(HttpServletResponse resp, PrintWriter out) throws Exception {
        List<KruizeBulkConfigEntry> configEntries = experimentDAO.loadAllBulkConfigs();
        List<BulkConfig> configs = new ArrayList<>();

        for (KruizeBulkConfigEntry entry : configEntries) {
            configs.add(entry.toBulkConfig());
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        out.write(objectMapper.writeValueAsString(configs));
    }

    /**
     * Trigger webhook notification for config update
     */
    private void triggerWebhook(BulkConfig config) {
        try {
            String webhookUrl = config.getWebhookUrl();
            String payload = objectMapper.writeValueAsString(config);

            LOGGER.info("Triggering webhook for config: {} to URL: {}", config.getConfigName(), webhookUrl);

            GenericRestApiClient client = new GenericRestApiClient();
            client.setBaseURL(webhookUrl);
            GenericRestApiClient.HttpResponseWrapper response = client.callKruizeAPI(payload);

            int statusCode = response != null ? response.getStatusCode() : -1;
            boolean isSuccessStatus = statusCode >= HttpServletResponse.SC_OK
                    && statusCode < HttpServletResponse.SC_MULTIPLE_CHOICES;
            if (isSuccessStatus) {
                LOGGER.info("Webhook triggered successfully for config: {}, status: {}",
                        config.getConfigName(), statusCode);
            } else {
                LOGGER.warn("Webhook returned non-success status for config: {}, status: {}",
                        config.getConfigName(), response != null ? statusCode : "null");
            }

        } catch (Exception e) {
            LOGGER.error("Failed to trigger webhook for config: {}, error: {}",
                    config.getConfigName(), e.getMessage(), e);
            // Don't fail the update if webhook fails - just log the error
        }
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse resp, PrintWriter out, int statusCode, String message) {
        try {
            resp.setStatus(statusCode);
            String errorJson = String.format("{\"error\":\"%s\",\"status\":%d}",
                    message.replace("\"", "\\\""), statusCode);
            out.write(errorJson);
        } catch (Exception e) {
            LOGGER.error("Error sending error response: {}", e.getMessage(), e);
        }
    }
}
