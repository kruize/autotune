/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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
package com.autotune.common.datasource;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.dataSourceMetadata.*;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;

/**
 * DataSourceMetadataOperator is an abstraction with CRUD operations to manage DataSourceMetadataInfo Object
 * representing JSON for a given data source
 * <p>
 *  TODO -
 *  object is currently stored in memory going forward need to store cluster metadata in Kruize DB
 *  Implement methods to support update and delete operations for periodic update of DataSourceMetadataInfo
 */
public class DataSourceMetadataOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataOperator.class);
    private static final DataSourceMetadataOperator dataSourceMetadataOperatorInstance = new DataSourceMetadataOperator();
    private DataSourceMetadataInfo dataSourceMetadataInfo;

    private DataSourceMetadataOperator() {
        this.dataSourceMetadataInfo = null;
    }

    public static DataSourceMetadataOperator getInstance() {
        return dataSourceMetadataOperatorInstance;
    }

    /**
     * Creates and populates metadata for a data source based on the provided DataSourceInfo object.
     * <p>
     * Currently supported DataSourceProvider - Prometheus
     *
     * @param dataSourceInfo   The DataSourceInfo object containing information about the data source.
     * @param uniqueKey        this is used as labels in query example container="xyz" namespace="abc"
     * @param startTime        Get metadata from starttime to endtime
     * @param endTime          Get metadata from starttime to endtime
     * @param steps            the interval between data points in a range query
     *                                                                                                                                                                                                                                                                                                 TODO - support multiple data sources
     * @param includeResources
     * @param excludeResources
     */
    public DataSourceMetadataInfo createDataSourceMetadata(String metadataProfileName, DataSourceInfo dataSourceInfo, String uniqueKey, long startTime,
                                                           long endTime, int steps,  int measurementDuration, Map<String, String> includeResources,
                                                           Map<String, String> excludeResources) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return processQueriesAndPopulateDataSourceMetadataInfo(metadataProfileName, dataSourceInfo, uniqueKey, startTime,
                endTime, steps, measurementDuration, includeResources, excludeResources);
    }

    /**
     * Retrieves DataSourceMetadataInfo object.
     *
     * @return DataSourceMetadataInfo containing metadata about the data source if found, otherwise null.
     */
    public DataSourceMetadataInfo getDataSourceMetadataInfo(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceMetadataInfo) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
                return null;
            }
            String dataSourceName = dataSourceInfo.getName();
            HashMap<String, DataSource> dataSourceHashMap = dataSourceMetadataInfo.getDatasources();

            if (null == dataSourceHashMap || !dataSourceHashMap.containsKey(dataSourceName)) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE + "{}", dataSourceName);
                return null;
            }

            DataSource targetDataSource = dataSourceHashMap.get(dataSourceName);
            HashMap<String, DataSource> targetDataSourceHashMap = new HashMap<>();
            targetDataSourceHashMap.put(dataSourceName, targetDataSource);
            return new DataSourceMetadataInfo(targetDataSourceHashMap);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    /**
     * Updates the metadata information of a data source with the provided DataSourceInfo object,
     * while preserving existing metadata information.
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the
     *                       data source to be updated.
     *                       <p>
     *                                                                                                                                                                                                                                                                          TODO - Currently Create and Update functions have identical functionalities, based on UI workflow and requirements
     *                                                                                                                                                                                                                                                                                 need to further enhance updateDataSourceMetadata() to support namespace, workload level granular updates
     */
    public DataSourceMetadataInfo updateDataSourceMetadata(String metadataProfileName,DataSourceInfo dataSourceInfo, String uniqueKey, long startTime,
                                                           long endTime, int steps, int measurementDuration, Map<String, String> includeResources,
                                                           Map<String, String> excludeResources) throws Exception {
        return processQueriesAndPopulateDataSourceMetadataInfo(metadataProfileName, dataSourceInfo, uniqueKey, startTime,
                endTime, steps, measurementDuration, includeResources, excludeResources);
    }

    /**
     * Deletes the metadata information of a data source with the provided DataSourceInfo object,
     *
     * @param dataSourceInfo The DataSourceInfo object containing information about the
     *                       metadata to be deleted.
     */
    public void deleteDataSourceMetadata(DataSourceInfo dataSourceInfo) {
        try {
            if (null == dataSourceMetadataInfo) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_NOT_AVAILABLE);
                return;
            }
            String dataSourceName = dataSourceInfo.getName();
            HashMap<String, DataSource> dataSourceHashMap = dataSourceMetadataInfo.getDatasources();

            if (null == dataSourceHashMap || !dataSourceHashMap.containsKey(dataSourceName)) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_DATASOURCE_NOT_AVAILABLE + "{}", dataSourceName);
            }

            dataSourceHashMap.remove(dataSourceName);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Fetches and processes metadata related to namespaces, workloads, and containers of a given datasource and populates the
     * DataSourceMetadataInfo object
     *
     * @param dataSourceInfo   The DataSourceInfo object containing information about the data source
     * @param uniqueKey        this is used as labels in query example container="xyz" namespace="abc"
     * @param startTime        Get metadata from starttime to endtime
     * @param endTime          Get metadata from starttime to endtime
     * @param steps            the interval between data points in a range query
     * @param includeResources
     * @param excludeResources
     * @return DataSourceMetadataInfo object with populated metadata fields
     * todo rename processQueriesAndFetchClusterMetadataInfo
     */
    public DataSourceMetadataInfo processQueriesAndPopulateDataSourceMetadataInfo(String metadataProfileName, DataSourceInfo dataSourceInfo, String uniqueKey,
                                                                                  long startTime, long endTime, int steps, int measurementDuration,
                                                                                  Map<String, String> includeResources,
                                                                                  Map<String, String> excludeResources) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        /**
         * Get DataSourceOperatorImpl instance on runtime based on dataSource provider
         */
        DataSourceOperatorImpl op = DataSourceOperatorImpl.getInstance().getOperator(dataSourceInfo.getProvider());

        if (null == op) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_OPERATOR_RETRIEVAL_FAILURE, dataSourceInfo.getProvider());
            return null;
        }
        /**
         * For the "prometheus" data source, fetches and processes data related to namespaces, workloads, and containers,
         * creating a comprehensive DataSourceMetadataInfo object that is then added to a list.
         * TODO - Process cluster metadata using a custom query
         */
        List<String> fields = Arrays.asList("namespace", "workload", "container");
        Map<String, String> queries = new HashMap<>();

        MetadataProfile metadataProfile = MetadataProfileCollection.getInstance().getMetadataProfileCollection().get(metadataProfileName);

        fields.forEach(field -> {
            String includeRegex = includeResources.getOrDefault(field + "Regex", "");
            String excludeRegex = excludeResources.getOrDefault(field + "Regex", "");
            String filter = constructDynamicFilter(field, includeRegex, excludeRegex);
            String queryTemplate = getQueryTemplate(field, metadataProfile);
            String filteredQuery;
            if (queryTemplate.contains("%s")) {
                filteredQuery = String.format(queryTemplate, filter);

            } else if (queryTemplate.contains(field + "!=\"\"")) {
                filteredQuery = queryTemplate.replace(
                    field + "!=\"\"",
                    filter.isEmpty() ? field + "!=\"\"" : filter
                );

            } else {
                LOGGER.warn(
                    "No injectable filter placeholder found for field {} in queryTemplate={}",
                    field,
                    queryTemplate
                );
                filteredQuery = queryTemplate; // fallback
            }

            queries.put(field, filteredQuery);
        });

        String namespaceQuery = queries.get("namespace");
        String workloadQuery = queries.get("workload");
        String containerQuery = queries.get("container");

        String dataSourceName = dataSourceInfo.getName();
        if (null != uniqueKey && !uniqueKey.isEmpty()) {
            LOGGER.debug("uniquekey: {}", uniqueKey);
            namespaceQuery = namespaceQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "," + uniqueKey);
            workloadQuery = workloadQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "," + uniqueKey);
            containerQuery = containerQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "," + uniqueKey);
        } else {
            namespaceQuery = namespaceQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "");
            workloadQuery = workloadQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "");
            containerQuery = containerQuery.replace(KruizeConstants.KRUIZE_BULK_API.ADDITIONAL_LABEL, "");
        }

        namespaceQuery = namespaceQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));
        workloadQuery = workloadQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));
        containerQuery = containerQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

        LOGGER.info("namespaceQuery: {}", namespaceQuery);
        LOGGER.info("workloadQuery: {}", workloadQuery);
        LOGGER.info("containerQuery: {}", containerQuery);

        JsonArray namespacesDataResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
        LOGGER.debug("namespacesDataResultArray: {}", namespacesDataResultArray);
        if (!op.validateResultArray(namespacesDataResultArray)) {
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, null);
        } else {
            HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
            LOGGER.debug("datasourceNamespaces: {}", datasourceNamespaces.keySet());
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, datasourceNamespaces);

            HashMap<String, HashMap<String, DataSourceWorkload>> datasourceWorkloads = new HashMap<>();
            JsonArray workloadDataResultArray = fetchQueryResults(dataSourceInfo, workloadQuery, startTime, endTime, steps);
            LOGGER.debug("workloadDataResultArray: {}", workloadDataResultArray);

            if (op.validateResultArray(workloadDataResultArray)) {
                datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);
            }
            dataSourceDetailsHelper.updateWorkloadDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads);

            HashMap<String, HashMap<String, DataSourceContainer>> datasourceContainers = new HashMap<>();
            JsonArray containerDataResultArray = fetchQueryResults(dataSourceInfo, containerQuery, startTime, endTime, steps);

            LOGGER.debug("containerDataResultArray: {}", containerDataResultArray);

            if (op.validateResultArray(containerDataResultArray)) {
                datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);
            }
            dataSourceDetailsHelper.updateContainerDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads, datasourceContainers);

            HashMap<String, DataSourceNamespace> matchedNamespaces = getMatchedNamespaces(dataSourceInfo, includeResources, startTime, endTime, steps, measurementDuration);
            HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = getMatchedWorkloads(dataSourceInfo, includeResources, startTime, endTime, steps, measurementDuration);

            dataSourceDetailsHelper.filterMetadataInfoObject(dataSourceName, dataSourceMetadataInfo, matchedNamespaces, matchedWorkloads);

            return getDataSourceMetadataInfo(dataSourceInfo);
        }

        return null;

    }

    // Helper function to map fields to query templates
    private String getQueryTemplate(String field, MetadataProfile metadataProfile) {
        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();

        return switch (field) {
            case "namespace" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.NAMESPACE);
            case "workload" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.WORKLOAD);
            case "container" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.CONTAINER);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
    }

    String constructDynamicFilter(String field, String includeRegex, String excludeRegex) {
        StringBuilder filterBuilder = new StringBuilder();
        if (includeRegex.isEmpty() && excludeRegex.isEmpty()) {
            filterBuilder.append(String.format("%s!=''", field));
        }
        if (!includeRegex.isEmpty()) {
            filterBuilder.append(String.format("%s=~\"%s\"", field, includeRegex));
        }
        if (!excludeRegex.isEmpty()) {
            if (!filterBuilder.isEmpty()) {
                filterBuilder.append(",");
            }
            filterBuilder.append(String.format("%s!~\"%s\"", field, excludeRegex));
        }
        LOGGER.info("filterBuilder: {}", filterBuilder);
        return filterBuilder.toString();
    }

    private HashMap<String, DataSourceNamespace> getMatchedNamespaces(DataSourceInfo dataSourceInfo, Map<String, String> includeResources,
                                                                      long startTime, long endTime, int steps, int measurementDuration) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, DataSourceNamespace> matchedNamespaces = new HashMap<>();
        String namespaceLabelFilter = includeResources.getOrDefault("namespaceLabelFilter", "");

        if (namespaceLabelFilter.isEmpty()) {
            return matchedNamespaces;
        }

        String namespaceQuery = String.format(
                "sum by (namespace) (max_over_time(kube_namespace_labels{%s}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                namespaceLabelFilter
        ).replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

        LOGGER.info("namespaceLabelFilterQuery: {}", namespaceQuery);
        JsonArray namespaceQueryResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
        return new DataSourceMetadataHelper().getActiveNamespaces(namespaceQueryResultArray);
    }

    private HashMap<String, HashMap<String, DataSourceWorkload>> getMatchedWorkloads(DataSourceInfo dataSourceInfo, Map<String, String> includeResources,
                                                                                      long startTime, long endTime, int steps, int measurementDuration) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = new HashMap<>();
        String podLabelFilter = includeResources.getOrDefault("podLabelFilter", "");

        if (podLabelFilter.isEmpty()) {
            return matchedWorkloads;
        }

        String workloadQuery = String.format(
                "sum by (namespace, workload, workload_type) (max_over_time(kube_pod_labels{pod!=\"\",%s}[$MEASUREMENT_DURATION_IN_MIN$m]) * on (namespace, pod) group_left(workload, workload_type) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]))",
                podLabelFilter
        ).replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

        LOGGER.info("podLabelFilterQuery: {}", workloadQuery);
        JsonArray workloadQueryResultArray = fetchQueryResults(dataSourceInfo, workloadQuery, startTime, endTime, steps);
        return new DataSourceMetadataHelper().getWorkloadInfo(workloadQueryResultArray);
    }

    private JsonArray fetchQueryResults(DataSourceInfo dataSourceInfo, String query, long startTime, long endTime, int steps) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        GenericRestApiClient client = new GenericRestApiClient(dataSourceInfo);
        String metricsUrl;
        if (startTime != 0 && endTime != 0 && steps != 0) {
            metricsUrl = String.format(KruizeConstants.DataSourceConstants.DATASOURCE_ENDPOINT_WITH_QUERY_RANGE,
                    dataSourceInfo.getUrl(),
                    URLEncoder.encode(query, CHARACTER_ENCODING),
                    startTime,
                    endTime,
                    steps);
        } else {
            metricsUrl = String.format(KruizeConstants.DataSourceConstants.DATE_ENDPOINT_WITH_QUERY,
                    dataSourceInfo.getUrl(),
                    URLEncoder.encode(query, CHARACTER_ENCODING)
            );
        }

        LOGGER.debug("MetricsUrl: {}", metricsUrl);
        client.setBaseURL(metricsUrl);
        JSONObject genericJsonObject = client.fetchMetricsJson(KruizeConstants.APIMessages.GET, "");
        JsonObject jsonObject = new Gson().fromJson(genericJsonObject.toString(), JsonObject.class);
        return jsonObject.getAsJsonObject(KruizeConstants.JSONKeys.DATA).getAsJsonArray(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.RESULT);
    }
}
