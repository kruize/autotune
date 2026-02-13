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

package com.autotune.analyzer.kruizeLayer;

import com.autotune.analyzer.exceptions.InvalidBoundsException;
import com.autotune.analyzer.kruizeLayer.presence.QueryBasedPresence;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import com.autotune.common.data.ValidationOutputData;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation helper for KruizeLayer objects
 */
public class LayerValidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerValidation.class);

    /**
     * Validates a KruizeLayer object
     *
     * @param layer The layer to validate
     * @return ValidationOutputData with success flag and error messages
     */
    public ValidationOutputData validate(KruizeLayer layer) {
        // Check for null layer object first
        if (layer == null) {
            LOGGER.error("Layer validation failed: layer is null");
            return new ValidationOutputData(false,
                    AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_NULL,
                    HttpServletResponse.SC_BAD_REQUEST);
        }

        List<String> errors = new ArrayList<>();

        // 1. Validate mandatory fields
        if (layer.getMetadata() == null || layer.getMetadata().getName() == null ||
            layer.getMetadata().getName().trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_METADATA_NAME_NULL);
        }

        if (layer.getLayerName() == null || layer.getLayerName().trim().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_NAME_NULL);
        }

        if (layer.getLayerPresence() == null) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_NULL);
        } else {
            // 2. Validate layer presence mutual exclusivity (only if not null)
            ValidationOutputData presenceValidation = validateLayerPresence(layer.getLayerPresence());
            if (!presenceValidation.isSuccess()) {
                errors.add(presenceValidation.getMessage());
            }
        }

        if (layer.getTunables() == null || layer.getTunables().isEmpty()) {
            errors.add(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_TUNABLES_NULL_OR_EMPTY);
        }

        // 3. Validate tunables
        if (layer.getTunables() != null && !layer.getTunables().isEmpty()) {
            ValidationOutputData tunableValidation = validateTunables(layer.getTunables());
            if (!tunableValidation.isSuccess()) {
                errors.add(tunableValidation.getMessage());
            }
        }

        // Return result
        if (!errors.isEmpty()) {
            String errorMsg = String.join("; ", errors);
            LOGGER.error("Layer validation failed: {}", errorMsg);
            return new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST);
        }

        // 4. Validate query execution for query-based layers
        ValidationOutputData queryValidation = validateQueryExecution(layer);
        if (!queryValidation.isSuccess()) {
            return queryValidation;
        }

        return new ValidationOutputData(true, null, null);
    }

    /**
     * Validates LayerPresence mutual exclusivity
     * Business rule: Must have EXACTLY ONE of: presence='always', queries, or label
     */
    private ValidationOutputData validateLayerPresence(LayerPresence presence) {
        int presenceTypeCount = 0;

        if (presence.getPresence() != null && !presence.getPresence().trim().isEmpty()) {
            presenceTypeCount++;
        }
        if (presence.getQueries() != null && !presence.getQueries().isEmpty()) {
            presenceTypeCount++;
        }
        if (presence.getLabel() != null && !presence.getLabel().isEmpty()) {
            presenceTypeCount++;
        }

        if (presenceTypeCount == 0) {
            return new ValidationOutputData(false,
                AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_MISSING,
                HttpServletResponse.SC_BAD_REQUEST);
        }

        if (presenceTypeCount > 1) {
            return new ValidationOutputData(false,
                AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_PRESENCE_MULTIPLE_TYPES,
                HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, null);
    }

    /**
     * Validates tunables collection
     * - Checks for duplicate tunable names
     * - Validates each tunable using Tunable.validate()
     */
    private ValidationOutputData validateTunables(ArrayList<Tunable> tunables) {
        List<String> errors = new ArrayList<>();

        // Check for null tunables in the list - early return if found
        if (tunables.contains(null)) {
            String errorMsg = AnalyzerErrorConstants.APIErrors.CreateLayerAPI.TUNABLE_NULL_IN_LIST;
            LOGGER.error("Tunable validation failed: {}", errorMsg);
            return new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST);
        }

        // Check for duplicate tunable names
        Set<String> tunableNames = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (Tunable tunable : tunables) {

            // Check for duplicate names
            if (tunable.getName() != null) {
                if (tunableNames.contains(tunable.getName())) {
                    duplicates.add(tunable.getName());
                } else {
                    tunableNames.add(tunable.getName());
                }
            }

            // Validate individual tunable using existing Tunable.validate() method
            try {
                tunable.validate();
            } catch (InvalidBoundsException e) {
                errors.add(e.getMessage());
            } catch (Exception e) {
                // Catch any unexpected exceptions during validation
                String errorMsg = String.format("Unexpected error validating tunable '%s': %s",
                        tunable.getName() != null ? tunable.getName() : "unknown",
                        e.getMessage());
                LOGGER.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        if (!duplicates.isEmpty()) {
            errors.add(String.format(AnalyzerErrorConstants.APIErrors.CreateLayerAPI.LAYER_DUPLICATE_TUNABLE_NAMES,
                    String.join(", ", duplicates)));
        }

        if (!errors.isEmpty()) {
            return new ValidationOutputData(false, String.join("; ", errors), HttpServletResponse.SC_BAD_REQUEST);
        }

        return new ValidationOutputData(true, null, null);
    }

    /**
     * Validates query execution for query-based layer presence
     * Executes ALL queries against their datasources to ensure they are syntactically valid
     *
     * @param layer The layer to validate (assumed to be non-null with valid layerPresence)
     * @return ValidationOutputData with success flag and error messages
     */
    private ValidationOutputData validateQueryExecution(KruizeLayer layer) {
        // Only validate if detector is QueryBasedPresence
        if (!(layer.getLayerPresence().getDetector() instanceof QueryBasedPresence)) {
            return new ValidationOutputData(true, null, null);
        }

        QueryBasedPresence queryDetector = (QueryBasedPresence) layer.getLayerPresence().getDetector();
        List<LayerPresenceQuery> queries = queryDetector.getQueries();

        // Skip if no queries defined
        if (queries == null || queries.isEmpty()) {
            LOGGER.warn(LogMessages.NO_QUERIES_DEFINED);
            return new ValidationOutputData(true, null, null);
        }

        LOGGER.debug("Validating query execution for layer: {}", layer.getLayerName());

        // Track if any query returned data and which datasources work
        boolean anyQueryReturnedData = false;
        Set<String> datasourcesWithData = new HashSet<>();

        // Validate ALL queries
        for (LayerPresenceQuery query : queries) {
            // Skip null query objects
            if (query == null) {
                LOGGER.warn(LogMessages.NULL_QUERY_ENCOUNTERED);
                continue;
            }

            // Execute the base query without any filters
            String baseQuery = query.getLayerPresenceQuery();
            LOGGER.debug("Validating query: {}", baseQuery);

            // Get all datasources
            List<DataSourceInfo> allDatasources = new ArrayList<>(
                    DataSourceCollection.getInstance().getDataSourcesCollection().values()
            );

            // Sort for deterministic order
            allDatasources.sort(Comparator.comparing(DataSourceInfo::getName));

            // Run for all datasources
            for (DataSourceInfo dataSourceInfo : allDatasources) {
                try {
                    // Get the appropriate operator for the datasource provider
                    DataSourceOperatorImpl operator = DataSourceOperatorImpl.getInstance()
                            .getOperator(dataSourceInfo.getProvider());

                    if (operator == null) {
                        LOGGER.warn(LogMessages.QUERY_VALIDATION_SKIP_NO_OPERATOR,
                                dataSourceInfo.getProvider(), dataSourceInfo.getName());
                        continue;
                    }

                    // Execute query against this datasource
                    JsonArray resultArray = operator.getResultArrayForQuery(dataSourceInfo, baseQuery);

                    // Check if this query returned data from this datasource
                    if (resultArray != null && !resultArray.isEmpty()) {
                        anyQueryReturnedData = true;
                        datasourcesWithData.add(dataSourceInfo.getName());
                        LOGGER.debug(LogMessages.QUERY_VALIDATION_SUCCESS_WITH_DATA,
                                resultArray.size(), dataSourceInfo.getName());
                    } else {
                        LOGGER.debug(LogMessages.QUERY_VALIDATION_SUCCESS_NO_DATA,
                                dataSourceInfo.getName());
                    }

                } catch (Exception e) {
                    // Query execution failed for this datasource - log and continue to next datasource
                    LOGGER.warn(LogMessages.QUERY_VALIDATION_FAILED_FOR_DATASOURCE,
                            dataSourceInfo.getName(), e.getMessage());
                }
            }
        }

        // After executing all queries against all datasources, check if any returned data
        if (!anyQueryReturnedData) {
            String errorMsg = String.format(LogMessages.QUERY_VALIDATION_NO_DATA_FROM_ANY_DATASOURCE,
                    layer.getLayerName());
            LOGGER.error(errorMsg);
            return new ValidationOutputData(false, errorMsg, HttpServletResponse.SC_BAD_REQUEST);
        }

        // Log which datasources can provide runtime recommendations
        LOGGER.info(LogMessages.QUERY_VALIDATION_COMPLETE_SUCCESS,
                layer.getLayerName(), datasourcesWithData);

        return new ValidationOutputData(true, null, null);
    }
}
