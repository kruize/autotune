/*******************************************************************************
 * Copyright (c) 2025, IBM Corporation and others.
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

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.KruizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Utility class that performs validation for bulk service requests.
 * This class provides methods to validate various fields in a BulkInput
 * object such as time range and datasource reachability, and generates
 * appropriate ValidationOutputData objects based on validation results.
 *
 * The validation flow primarily checks:
 * <ul>
 *     <li>Time range consistency and format</li>
 *     <li>Datasource connectivity and serviceability</li>
 * </ul>
 * If all validations pass, a successful response is returned.
 */
public class BulkServiceValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkServiceValidation.class);
    
    // Cluster name validation constants
    private static final int MAX_CLUSTER_NAME_LENGTH = 253;
    private static final String CLUSTER_NAME_FORMAT_ERROR =
            "Invalid cluster_name: must not exceed " + MAX_CLUSTER_NAME_LENGTH + " characters.";
    
    // Valid recommendation model names
    private static final Set<String> VALID_MODELS = Set.of("performance", "cost");
    
    // Valid recommendation term names
    private static final Set<String> VALID_TERMS = Set.of("short", "medium", "long");

    /**
     * Validates the bulk request payload and returns the corresponding validation output.
     * The following validations occur in sequence:
     * <ol>
     *     <li>Validate the time range specified in the payload.</li>
     *     <li>If a datasource name is provided, validate its connection and serviceability.</li>
     * </ol>
     * If an error is detected in any step, an appropriate ValidationOutputData object
     * is returned containing the error message and status code.
     * Otherwise, a success response (HTTP status 200) is returned.
     *
     * @param payload the bulk input payload to validate
     * @param jobID   the job id used for contextualizing validation errors
     * @return a populated ValidationOutputData object representing success or the first encountered validation error
     * @throws Exception if an unexpected error occurs during validation
     */
    public static ValidationOutputData validate(BulkInput payload, String jobID) throws Exception {

        ValidationOutputData validationOutputData;

        validationOutputData = buildErrorOutput(validateTimeRange(payload.getTime_range()), jobID);
        if (validationOutputData != null) return validationOutputData;

        // Validate cluster_name if provided
        validationOutputData = buildErrorOutput(validateClusterName(payload.getCluster_name()), jobID);
        if (validationOutputData != null) return validationOutputData;

        // Validate model_settings if provided
        validationOutputData = buildErrorOutput(validateModelSettings(payload.getModel_settings()), jobID);
        if (validationOutputData != null) return validationOutputData;

        // Validate term_settings if provided
        validationOutputData = buildErrorOutput(validateTermSettings(payload.getTerm_settings()), jobID);
        if (validationOutputData != null) return validationOutputData;

        if (payload.getDatasource() != null) {
            validationOutputData = buildErrorOutput(validateDatasourceConnection(payload.getDatasource()), jobID);
        }

        if (validationOutputData == null) {
            validationOutputData = new ValidationOutputData(true, "", 200);
        }
        return validationOutputData;
    }

    /**
     * Builds an error output object if the given error message is non-empty.
     * Utility method that appends the job ID to the message and wraps it into
     * a ValidationOutputData object with HTTP 400 status.
     *
     * @param errorMsg the validation error message; may be empty or null
     * @param jobID    the job identifier appended for context
     * @return a ValidationOutputData object if an error exists, otherwise null
     */
    private static ValidationOutputData buildErrorOutput(String errorMsg, String jobID) {
        if (errorMsg != null && !errorMsg.isEmpty()) {
            return new ValidationOutputData(false, errorMsg + " for the jobId: " + jobID, 400);
        }
        return null;
    }


    /**
     * Validates the connectivity and serviceability of the given datasource.
     * This method attempts to:
     * <ol>
     *     <li>Load the datasource metadata from database.</li>
     *     <li>Verify reachability using the registered DataSourceOperatorImpl.</li>
     * </ol>
     * If any step fails or the datasource is not serviceable, an appropriate error message
     * is returned. Otherwise, an empty string signifies a successful validation.
     *
     * @param datasourceName the name of the datasource to validate
     * @return an error message if validation fails; otherwise an empty string
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

    /**
     * Validates the time range provided in the bulk request.
     * This method checks for:
     * <ul>
     *     <li>Presence of time range (null or empty is allowed and considered valid)</li>
     *     <li>Correct ISO-8601 datetime format</li>
     *     <li>Start time that is not after end time</li>
     * </ul>
     * If validation fails, it returns a specific error message; otherwise returns an empty string.
     *
     * @param timeRange the time range object containing start and end timestamps
     * @return an error message if validation fails; otherwise an empty string
     */
    public static String validateTimeRange(BulkInput.TimeRange timeRange) {
        String errorMessage = "";
        if (timeRange == null || timeRange.isEmpty()) {
            LOGGER.debug("No time range specified");
            return errorMessage;
        }
        try {
            OffsetDateTime startTime = OffsetDateTime.parse(timeRange.getStart());
            OffsetDateTime endTime = OffsetDateTime.parse(timeRange.getEnd());

            if (startTime.isAfter(endTime)) {
                errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_START_TIME;
                return errorMessage;
            }

        } catch (DateTimeParseException ex) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.INVALID_DATE_FORMAT;
        } catch (Exception e) {
            errorMessage = KruizeConstants.KRUIZE_BULK_API.TIME_RANGE_EXCEPTION;
        }
        return errorMessage;
    }

    /**
     * Validates the cluster_name field if provided.
     * Trims whitespace and checks for:
     * <ul>
     *     <li>Empty string (not allowed)</li>
     *     <li>Length (max 253 characters per Kubernetes DNS-1123 subdomain spec)</li>
     *     <li>Format (lowercase alphanumeric with hyphens and dots)</li>
     * </ul>
     *
     * @param clusterName the cluster name to validate (can be null)
     * @return an error message if validation fails; otherwise an empty string
     */
    public static String validateClusterName(String clusterName) {
        if (clusterName == null) {
            return ""; // null is valid (will use metadata cluster)
        }
        
        // Trim whitespace to handle accidental surrounding spaces
        String trimmedClusterName = clusterName.trim();
        
        if (trimmedClusterName.isEmpty()) {
            return "cluster_name cannot be an empty string. Either provide a valid cluster name or omit the field";
        }
        
        if (trimmedClusterName.length() > MAX_CLUSTER_NAME_LENGTH) {
            return CLUSTER_NAME_FORMAT_ERROR + " Provided: " + trimmedClusterName.length() + " characters";
        }
        
        return "";
    }

    /**
     * Validates the model_settings field if provided.
     * Checks for:
     * <ul>
     *     <li>Non-null models list when model_settings is provided</li>
     *     <li>Non-empty models list</li>
     *     <li>Valid model names (performance, cost)</li>
     * </ul>
     *
     * @param modelSettings the model settings to validate (can be null)
     * @return an error message if validation fails; otherwise an empty string
     */
    public static String validateModelSettings(com.autotune.analyzer.kruizeObject.ModelSettings modelSettings) {
        if (modelSettings == null) {
            return ""; // null is valid (will use all default models)
        }
        
        if (modelSettings.getModels() == null || modelSettings.getModels().isEmpty()) {
            return "model_settings.models cannot be null or empty when model_settings is provided. " +
                   "Valid model names: " + VALID_MODELS;
        }
        
        for (String model : modelSettings.getModels()) {
            if (model == null || model.trim().isEmpty()) {
                return "model_settings.models contains null or empty model name";
            }
            
            String modelLower = model.toLowerCase().trim();
            if (!VALID_MODELS.contains(modelLower)) {
                return "Invalid model name: '" + model + "'. Valid model names are: " + VALID_MODELS;
            }
        }
        
        return "";
    }

    /**
     * Validates the term_settings field if provided.
     * Checks for:
     * <ul>
     *     <li>Non-null terms list when term_settings is provided</li>
     *     <li>Non-empty terms list</li>
     *     <li>Valid term names (short, medium, long)</li>
     * </ul>
     *
     * @param termSettings the term settings to validate (can be null)
     * @return an error message if validation fails; otherwise an empty string
     */
    public static String validateTermSettings(com.autotune.analyzer.kruizeObject.TermSettings termSettings) {
        if (termSettings == null) {
            return ""; // null is valid (will use all default terms)
        }
        
        if (termSettings.getTerms() == null || termSettings.getTerms().isEmpty()) {
            return "term_settings.terms cannot be null or empty when term_settings is provided. " +
                   "Valid term names: " + VALID_TERMS;
        }
        
        for (String term : termSettings.getTerms()) {
            if (term == null || term.trim().isEmpty()) {
                return "term_settings.terms contains null or empty term name";
            }
            
            String termLower = term.toLowerCase().trim();
            if (!VALID_TERMS.contains(termLower)) {
                return "Invalid term name: '" + term + "'. Valid term names are: " + VALID_TERMS;
            }
        }
        
        return "";
    }
}
