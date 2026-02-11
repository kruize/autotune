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

package com.autotune.analyzer.kruizeLayer.presence;

import com.autotune.analyzer.kruizeLayer.LayerPresenceQuery;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.PresenceType;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for query-based layer presence detection
 */
public class QueryBasedPresence implements LayerPresenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBasedPresence.class);

    private List<LayerPresenceQuery> queries;

    public QueryBasedPresence() {
        this.queries = new ArrayList<>();
    }

    public QueryBasedPresence(List<LayerPresenceQuery> queries) {
        this.queries = queries != null ? queries : new ArrayList<>();
    }

    @Override
    public PresenceType getType() {
        return PresenceType.QUERY;
    }

    @Override
    public boolean detectPresence(String namespace, String workloadName) throws Exception {
        return detectPresence(namespace, workloadName, null);
    }

    /**
     * Detect layer presence with optional container name filtering
     * @param namespace The Kubernetes namespace
     * @param workloadName The workload name
     * @param containerName The container name (optional)
     * @return true if layer is detected
     * @throws Exception if detection fails
     */
    public boolean detectPresence(String namespace, String workloadName, String containerName) throws Exception {
        if (queries == null || queries.isEmpty()) {
            LOGGER.warn(LogMessages.NO_QUERIES_DEFINED);
            return false;
        }

        // Iterate through all configured queries
        for (LayerPresenceQuery query : queries) {
            // Skip null query objects
            if (query == null) {
                LOGGER.warn(LogMessages.NULL_QUERY_ENCOUNTERED);
                continue;
            }

            try {
                // Get datasource info from global collection
                DataSourceInfo dataSourceInfo = DataSourceCollection.getInstance()
                        .getDataSourcesCollection()
                        .get(query.getDataSource());

                if (dataSourceInfo == null) {
                    LOGGER.warn(LogMessages.DATASOURCE_NOT_FOUND, query.getDataSource());
                    continue;
                }

                // Get the appropriate operator for the datasource
                DataSourceOperatorImpl operator = DataSourceOperatorImpl.getInstance()
                        .getOperator(query.getDataSource());

                if (operator == null) {
                    LOGGER.warn(LogMessages.NO_OPERATOR_AVAILABLE, query.getDataSource());
                    continue;
                }

                // Start with the original query
                String modifiedQuery = query.getLayerPresenceQuery();

                // Append dynamic filters for namespace, workload, and container
                if (namespace != null && !namespace.isBlank()) {
                    modifiedQuery = appendFilter(modifiedQuery, LayerConstants.LABEL_NAMESPACE, namespace);
                }
                if (workloadName != null && !workloadName.isBlank()) {
                    modifiedQuery = appendFilter(modifiedQuery, LayerConstants.LABEL_POD, workloadName);
                }
                if (containerName != null && !containerName.isBlank()) {
                    modifiedQuery = appendFilter(modifiedQuery, LayerConstants.LABEL_CONTAINER, containerName);
                }

                LOGGER.debug(LogMessages.EXECUTING_QUERY, modifiedQuery);

                // Execute the modified query and get results
                JsonArray resultArray = operator.getResultArrayForQuery(
                        dataSourceInfo,
                        modifiedQuery
                );

                // Check if we got any results - if yes, layer is present
                if (resultArray != null && resultArray.size() > 0) {
                    LOGGER.debug(LogMessages.LAYER_DETECTED_VIA_QUERY, namespace, workloadName, containerName);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error(LogMessages.ERROR_EXECUTING_QUERY, query.getDataSource(), e);
                // Continue to next query instead of failing completely
            }
        }

        // No queries returned positive results
        return false;
    }

    /**
     * Escapes special characters in a PromQL label value
     * PromQL requires backslashes and double quotes to be escaped
     *
     * @param value The label value to escape
     * @return Escaped label value safe for use in PromQL queries
     */
    private String escapePromQLLabelValue(String value) {
        if (value == null) {
            return "";
        }

        // Escape backslashes first (must be done before escaping quotes)
        String escaped = value.replace("\\", "\\\\");

        // Escape double quotes
        escaped = escaped.replace("\"", "\\\"");

        // Escape newlines
        escaped = escaped.replace("\n", "\\n");

        // Escape tabs
        escaped = escaped.replace("\t", "\\t");

        // Escape carriage returns
        escaped = escaped.replace("\r", "\\r");

        return escaped;
    }

    /**
     * Appends a label filter to a PromQL query
     *
     * @param query The original PromQL query
     * @param labelName The label name to filter by
     * @param labelValue The label value
     * @return Modified query with the filter appended
     */
    private String appendFilter(String query, String labelName, String labelValue) {
        if (query == null || query.isBlank()) {
            return query;
        }
        if (labelName == null || labelName.isBlank()) {
            return query;
        }
        if (labelValue == null || labelValue.isBlank()) {
            return query;
        }

        // Escape the label value to prevent PromQL injection and syntax errors
        String escapedValue = escapePromQLLabelValue(labelValue);
        String filter = labelName + "=\"" + escapedValue + "\"";

        // Find the first opening and closing brace
        int openBrace = query.indexOf('{');
        int closeBrace = query.indexOf('}');

        if (openBrace != -1 && closeBrace != -1 && closeBrace > openBrace) {
            // Braces exist - check if they contain content
            String existingFilters = query.substring(openBrace + 1, closeBrace).trim();

            if (existingFilters.isEmpty()) {
                // Empty braces - insert filter without comma
                return query.substring(0, openBrace + 1) + filter + query.substring(closeBrace);
            } else {
                // Braces with content - append with comma
                return query.substring(0, closeBrace) + "," + filter + query.substring(closeBrace);
            }
        } else {
            // No braces found - find the first space or end of metric name
            int spaceIndex = query.indexOf(' ');
            int insertPoint = (spaceIndex != -1) ? spaceIndex : query.length();

            // Insert braces with filter after the metric name
            return query.substring(0, insertPoint) + "{" + filter + "}" + query.substring(insertPoint);
        }
    }

    public List<LayerPresenceQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<LayerPresenceQuery> queries) {
        this.queries = queries != null ? queries : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "QueryBasedPresence{" +
                "queries=" + queries +
                '}';
    }
}
