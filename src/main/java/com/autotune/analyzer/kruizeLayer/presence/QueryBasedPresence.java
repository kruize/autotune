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
import com.autotune.analyzer.kruizeLayer.utils.LayerUtils;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.PresenceType;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceOperatorImpl;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
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

    /**
     * Detect layer presence using namespace and container name filtering
     * @param namespace The Kubernetes namespace
     * @param containerName The container name
     * @return true if layer is detected
     * @throws Exception if detection fails
     */
    @Override
    public boolean detectPresence(String namespace, String containerName, List<String> datasourceNames) throws Exception {
        if (queries == null || queries.isEmpty()) {
            LOGGER.warn(LogMessages.NO_QUERIES_DEFINED);
            return false;
        }

        if (datasourceNames == null || datasourceNames.isEmpty()) {
            LOGGER.warn("No datasource names provided for layer detection");
            return false;
        }

        // Iterate through all configured queries
        for (LayerPresenceQuery query : queries) {
            // Skip null query objects
            if (query == null) {
                LOGGER.warn(LogMessages.NULL_QUERY_ENCOUNTERED);
                continue;
            }

            for (String datasourceName : datasourceNames) {
                try {
                    // Get the specific datasource by name from the experiment
                    DataSourceInfo dataSourceInfo = DataSourceCollection.getInstance()
                            .getDataSourcesCollection()
                            .get(datasourceName);

                    if (dataSourceInfo == null) {
                        LOGGER.warn(LogMessages.DATASOURCE_NOT_FOUND, datasourceName);
                        continue;
                    }

                    // Skip queries that don't match the datasource provider
                    if (query.getDataSource() != null &&
                        !query.getDataSource().equalsIgnoreCase(dataSourceInfo.getProvider())) {
                        LOGGER.debug("Skipping query for datasource '{}' as it doesn't match current datasource provider '{}'",
                                query.getDataSource(), dataSourceInfo.getProvider());
                        continue;
                    }

                    // Get the appropriate operator for the datasource provider
                    DataSourceOperatorImpl operator = DataSourceOperatorImpl.getInstance()
                            .getOperator(dataSourceInfo.getProvider());

                    if (operator == null) {
                        LOGGER.warn(LogMessages.NO_OPERATOR_AVAILABLE, dataSourceInfo.getProvider());
                        continue;
                    }

                    // Start with the original query
                    String modifiedQuery = query.getLayerPresenceQuery();

                    if (KruizeConstants.SupportedDatasources.CRYOSTAT.equalsIgnoreCase(query.getDataSource())) {
                        // For Cryostat detection, we need to:
                        // 1. Query Prometheus to get the list of pods
                        // 2. Query Cryostat to check if those pods have JVM targets

                        DataSourceInfo promDatasourceInfo = null;
                        for (String experimentDatasourceName : datasourceNames) {
                            DataSourceInfo experimentDatasourceInfo = DataSourceCollection.getInstance()
                                    .getDataSourcesCollection()
                                    .get(experimentDatasourceName);
                            if (experimentDatasourceInfo != null &&
                                    KruizeConstants.SupportedDatasources.PROMETHEUS.equalsIgnoreCase(experimentDatasourceInfo.getProvider())) {
                                promDatasourceInfo = experimentDatasourceInfo;
                                break;
                            }
                        }

                        if (promDatasourceInfo == null) {
                            LOGGER.warn("Cryostat layer detection requires a Prometheus datasource instance in the experiment datasource list, but none found. Skipping Cryostat detection.");
                            continue;
                        }

                        DataSourceOperatorImpl prometheusOperator = DataSourceOperatorImpl.getInstance()
                                .getOperator(promDatasourceInfo.getProvider());
                        if (prometheusOperator == null) {
                            LOGGER.warn(
                                    "No datasource operator found for provider='{}' while performing Cryostat layer detection. " +
                                            "Skipping detection for datasource='{}'.", promDatasourceInfo.getProvider(),
                                    promDatasourceInfo.getName()
                            );
                            continue;
                        }

                        // Build PromQL query to get pods
                        String promQl = KruizeConstants.PromQueries.GET_PODS_WITH_NS_CONTAINER;
                        if (namespace != null && !namespace.isBlank()) {
                            promQl = appendFilter(promQl, LayerConstants.LABEL_NAMESPACE, namespace);
                        }
                        if (containerName != null && !containerName.isBlank()) {
                            promQl = appendFilter(promQl, LayerConstants.LABEL_CONTAINER, containerName);
                        }
                        LOGGER.debug("PromQl: {}", promQl);

                        // Execute Prometheus query using the Prometheus datasource
                        JSONObject returnObj = prometheusOperator.getJsonObjectForQuery(promDatasourceInfo, promQl);
                        List<String> pods = LayerUtils.extractPods(returnObj);

                        // Get Cryostat operator and datasource
                        DataSourceOperatorImpl cryostatOperator = DataSourceOperatorImpl.getInstance()
                                .getOperator(KruizeConstants.SupportedDatasources.CRYOSTAT);
                        if (cryostatOperator == null) {
                            LOGGER.warn(
                                    "No datasource operator found for provider='{}' while performing Cryostat layer detection. " +
                                            "Skipping detection for datasource='{}'.", KruizeConstants.SupportedDatasources.CRYOSTAT,
                                    dataSourceInfo.getName()
                            );
                            continue;
                        }
                        if (!pods.isEmpty()) {
                            for (String pod: pods) {
                                LOGGER.debug("Checking Cryostat targets for pod: {}", pod);
                                String queryToTry = modifiedQuery.replace("$POD_NAME$", pod);
                                LOGGER.debug("Cryostat Query: {}", queryToTry);

                                // Use the Cryostat datasource (dataSourceInfo) for GraphQL query
                                JSONObject graphQlJson = cryostatOperator.getJsonObjectForQuery(dataSourceInfo, queryToTry);
                                if (null == graphQlJson) {
                                    LOGGER.warn(
                                            "Cryostat query returned no response while checking layer presence. datasource='{}', provider='{}', namespace='{}', container='{}', pod='{}'. Skipping this pod.",
                                            dataSourceInfo.getName(),
                                            dataSourceInfo.getProvider(),
                                            namespace,
                                            containerName,
                                            pod
                                    );
                                    LOGGER.debug("Cryostat GraphQL query with no response: {}", queryToTry);
                                    continue;
                                }

                                LOGGER.debug("GraphQL object: {}", graphQlJson);
                                JSONArray envNodes = graphQlJson
                                        .optJSONObject("data")
                                        .optJSONArray("environmentNodes");

                                if (envNodes != null && !envNodes.isEmpty()) {
                                    LOGGER.debug("SUCCESS: Found Cryostat target(s) for pod '{}'", pod);
                                    return true;
                                }
                            }
                        }

                    } else {
                        // Append dynamic filters for namespace, container
                        if (namespace != null && !namespace.isBlank()) {
                            modifiedQuery = appendFilter(modifiedQuery, LayerConstants.LABEL_NAMESPACE, namespace);
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
                        if (resultArray != null && !resultArray.isEmpty()) {
                            LOGGER.debug(LogMessages.LAYER_DETECTED_VIA_QUERY, namespace, containerName);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(LogMessages.ERROR_EXECUTING_QUERY, query.getDataSource(), e);
                }
            }
        }

        // No queries returned positive results
        return false;
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
        String filter = labelName + "=\"" + labelValue.replace("\\", "\\\\").replace("\"","\\\"").replace("\n", "\\n").replace("\t", "\\t").replace("\r","\\r") + "\"";

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
