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

        // OPTIMIZATION: Apply label filters FIRST before fetching all metadata
        // This reduces the amount of data we need to fetch and process
        LOGGER.info("Applying label filters first to identify matching resources...");
        HashMap<String, DataSourceNamespace> matchedNamespaces = getNamespacesMatchingLabelFilter(dataSourceInfo, metadataProfile, includeResources, startTime, endTime, steps, measurementDuration);
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = getWorkloadsMatchingPodLabelFilter(dataSourceInfo, metadataProfile, includeResources, startTime, endTime, steps, measurementDuration);
        
        LOGGER.info("Label filter results - Matched namespaces: {}, Matched workloads: {}",
                    matchedNamespaces.size(),
                    matchedWorkloads.values().stream().mapToInt(Map::size).sum());

        // Now fetch full metadata (namespace/workload/container queries)
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

            // Apply the label filter results to remove non-matching resources
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

    /**
     * Fetches namespaces that match the specified namespace label filter.
     * Uses the namespacesWithLabelFilter query from the metadata profile.
     *
     * @param dataSourceInfo The data source information
     * @param metadataProfile The metadata profile containing the query
     * @param includeResources Map containing the namespaceLabelFilter
     * @param startTime Start time for the query
     * @param endTime End time for the query
     * @param steps Query step interval
     * @param measurementDuration Measurement duration in minutes
     * @return HashMap of namespaces matching the label filter
     */
    private HashMap<String, DataSourceNamespace> getNamespacesMatchingLabelFilter(DataSourceInfo dataSourceInfo, MetadataProfile metadataProfile,
                                                                                   Map<String, String> includeResources, long startTime, long endTime,
                                                                                   int steps, int measurementDuration) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, DataSourceNamespace> matchedNamespaces = new HashMap<>();
        String namespaceLabelFilter = includeResources.getOrDefault("namespaceLabelFilter", "");

        if (namespaceLabelFilter.isEmpty()) {
            return matchedNamespaces;
        }

        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        String queryTemplate = dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, "namespacesWithLabelFilter");
        
        if (queryTemplate == null) {
            LOGGER.warn("namespacesWithLabelFilter query not found in metadata profile, skipping namespace label filtering");
            return matchedNamespaces;
        }

        // Handle multiple labels with OR logic - split by comma and run separate queries
        String[] labelFilters = namespaceLabelFilter.split(",");
        LOGGER.info("namespaceLabelFilter has {} label(s): {}", labelFilters.length, namespaceLabelFilter);
        
        for (int i = 0; i < labelFilters.length; i++) {
            String singleLabelFilter = labelFilters[i].trim();
            String namespaceQuery = queryTemplate
                    .replace("LABEL_FILTER", singleLabelFilter)
                    .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

            LOGGER.info("Executing namespace label filter query {} of {}: {}", i+1, labelFilters.length, namespaceQuery);
            JsonArray namespaceQueryResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
            LOGGER.info("Query {} returned {} results", i+1, namespaceQueryResultArray != null ? namespaceQueryResultArray.size() : 0);
            
            HashMap<String, DataSourceNamespace> currentMatches = dataSourceDetailsHelper.getActiveNamespaces(namespaceQueryResultArray);
            LOGGER.info("Query {} matched {} namespace(s): {}", i+1, currentMatches.size(), currentMatches.keySet());
            
            // Merge results (OR logic - add all matches from each label)
            matchedNamespaces.putAll(currentMatches);
        }
        
        LOGGER.info("Total matched namespaces after OR logic: {} - {}", matchedNamespaces.size(), matchedNamespaces.keySet());
        return matchedNamespaces;
    }

    /**
     * Fetches workloads whose pods match the specified pod label filter.
     * Uses the workloadsWithPodLabelFilter query from the metadata profile.
     *
     * @param dataSourceInfo The data source information
     * @param metadataProfile The metadata profile containing the query
     * @param includeResources Map containing the podLabelFilter
     * @param startTime Start time for the query
     * @param endTime End time for the query
     * @param steps Query step interval
     * @param measurementDuration Measurement duration in minutes
     * @return HashMap of workloads whose pods match the label filter
     */
    private HashMap<String, HashMap<String, DataSourceWorkload>> getWorkloadsMatchingPodLabelFilter(DataSourceInfo dataSourceInfo, MetadataProfile metadataProfile,
                                                                                                     Map<String, String> includeResources, long startTime, long endTime,
                                                                                                     int steps, int measurementDuration) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = new HashMap<>();
        String podLabelFilter = includeResources.getOrDefault("podLabelFilter", "");

        if (podLabelFilter.isEmpty()) {
            return matchedWorkloads;
        }

        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        String queryTemplate = dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, "workloadsWithPodLabelFilter");
        
        if (queryTemplate == null) {
            LOGGER.warn("workloadsWithPodLabelFilter query not found in metadata profile, using hardcoded query");
            // Query with 'unless' clause to prevent deployment-managed pods from being treated as static pods
            // This fixes the duplicate experiment issue while still supporting true static pods
            queryTemplate = "sum by (namespace, workload, workload_type) ("
                    + "max_over_time(kube_pod_labels{pod!=\"\",LABEL_FILTER}[$MEASUREMENT_DURATION_IN_MIN$m]) "
                    + "* on (namespace, pod) group_left(workload, workload_type) "
                    + "max_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m])"
                    + ") or sum by (namespace, workload, workload_type) ("
                    + "label_replace("
                    + "label_replace("
                    + "max_over_time(kube_pod_labels{pod!=\"\",LABEL_FILTER}[$MEASUREMENT_DURATION_IN_MIN$m]) "
                    + "unless on (namespace, pod) max_over_time(namespace_workload_pod:kube_pod_owner:relabel{workload!=\"\"}[$MEASUREMENT_DURATION_IN_MIN$m]), "
                    + "\"workload\", \"$1\", \"pod\", \"(.*)\")"
                    + ", \"workload_type\", \"Pod\", \"\", \"\")"
                    + ")";
        }

        // Handle multiple labels with OR logic - split by comma and run separate queries
        String[] labelFilters = podLabelFilter.split(",");
        LOGGER.info("podLabelFilter has {} label(s): {}", labelFilters.length, podLabelFilter);
        
        for (int i = 0; i < labelFilters.length; i++) {
            String singleLabelFilter = labelFilters[i].trim();
            String workloadQuery = queryTemplate
                    .replace("LABEL_FILTER", singleLabelFilter)
                    .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

            LOGGER.info("Executing pod label filter query {} of {}: {}", i+1, labelFilters.length, workloadQuery);
            JsonArray workloadQueryResultArray = fetchQueryResults(dataSourceInfo, workloadQuery, startTime, endTime, steps);
            LOGGER.info("Query {} returned {} results", i+1, workloadQueryResultArray != null ? workloadQueryResultArray.size() : 0);
            
            HashMap<String, HashMap<String, DataSourceWorkload>> currentMatches = dataSourceDetailsHelper.getWorkloadInfo(workloadQueryResultArray);
            int currentWorkloadCount = currentMatches.values().stream().mapToInt(Map::size).sum();
            LOGGER.info("Query {} matched {} workload(s)", i+1, currentWorkloadCount);
            
            // Merge results (OR logic - add all matches from each label)
            currentMatches.forEach((namespace, workloads) -> {
                LOGGER.debug("Merging {} workload(s) from namespace {}: {}", workloads.size(), namespace, workloads.keySet());
                matchedWorkloads.computeIfAbsent(namespace, k -> new HashMap<>()).putAll(workloads);
            });
        }
        
        int totalWorkloads = matchedWorkloads.values().stream().mapToInt(Map::size).sum();
        LOGGER.info("Total matched workloads after OR logic: {}", totalWorkloads);
        matchedWorkloads.forEach((ns, wls) -> {
            LOGGER.info("  Namespace {}: {} workload(s) - {}", ns, wls.size(), wls.keySet());
        });
        return matchedWorkloads;
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
