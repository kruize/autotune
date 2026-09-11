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
     * @param startTime        Get metadata from starttime to endtime
     * @param endTime          Get metadata from starttime to endtime
     * @param steps            the interval between data points in a range query
     *                                                                                                                                                                                                                                                                                                 TODO - support multiple data sources
     * @param includeResources
     * @param excludeResources
     */
    public DataSourceMetadataInfo createDataSourceMetadata(String metadataProfileName, DataSourceInfo dataSourceInfo, long startTime,
                                                           long endTime, int steps,  int measurementDuration, Map<String, String> includeResources,
                                                           Map<String, String> excludeResources) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return processQueriesAndPopulateDataSourceMetadataInfo(metadataProfileName, dataSourceInfo, startTime,
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
    public DataSourceMetadataInfo updateDataSourceMetadata(String metadataProfileName,DataSourceInfo dataSourceInfo, long startTime,
                                                           long endTime, int steps, int measurementDuration, Map<String, String> includeResources,
                                                           Map<String, String> excludeResources) throws Exception {
        return processQueriesAndPopulateDataSourceMetadataInfo(metadataProfileName, dataSourceInfo, startTime,
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
     * @param startTime        Get metadata from starttime to endtime
     * @param endTime          Get metadata from starttime to endtime
     * @param steps            the interval between data points in a range query
     * @param includeResources
     * @param excludeResources
     * @return DataSourceMetadataInfo object with populated metadata fields
     * todo rename processQueriesAndFetchClusterMetadataInfo
     */
    public DataSourceMetadataInfo processQueriesAndPopulateDataSourceMetadataInfo(String metadataProfileName, DataSourceInfo dataSourceInfo,
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
        // Keys for the map
        List<String> fields = Arrays.asList("namespace", "workload", "container");
        // Map for storing queries
        Map<String, String> queries = new HashMap<>();

        MetadataProfile metadataProfile = MetadataProfileCollection.getInstance().getMetadataProfileCollection().get(metadataProfileName);

        if (null == metadataProfile) {
            LOGGER.error("Metadata profile '{}' not found in MetadataProfileCollection", metadataProfileName);
            return null;
        }

        // Get query templates for each field
        String namespaceQueryTemplate = getQueryTemplate("namespace", metadataProfile);
        String workloadQueryTemplate;
        String containerQueryTemplate = getQueryTemplate("container", metadataProfile);

        // Determine if pod label filters are present — if so, use label-aware workload template
        String includePodLabelFilter = includeResources.getOrDefault("podLabelFilter", "");
        String excludePodLabelFilter = excludeResources.getOrDefault("podLabelFilter", "");
        boolean hasLabelFilter = !includePodLabelFilter.isEmpty() || !excludePodLabelFilter.isEmpty();

        if (hasLabelFilter) {
            workloadQueryTemplate = dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER);
            if (workloadQueryTemplate == null) {
                LOGGER.error("Pod label filtering requested but '{}' query not found in metadata profile '{}'",
                    AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER, metadataProfileName);
                return null;
            }
            LOGGER.info("Label filter present — using {} template for workload query", AnalyzerConstants.WORKLOAD_METADATA_QUERY_WITH_LABEL_FILTER);
        } else {
            workloadQueryTemplate = getQueryTemplate("workload", metadataProfile);
        }

        // Validate all templates are present
        if (namespaceQueryTemplate == null || workloadQueryTemplate == null || containerQueryTemplate == null) {
            LOGGER.error("One or more query templates could not be resolved for metadata profile '{}', aborting metadata fetch", metadataProfileName);
            return null;
        }

        // Build named filters for each query type
        String namespaceFilters = buildNamespaceFilters(includeResources, excludeResources);
        String workloadFilters = buildWorkloadFilters(includeResources, excludeResources);
        String containerFilters = buildContainerFilters(includeResources, excludeResources);
        String labels = buildLabels(includeResources, excludeResources);

        // Replace named placeholders in queries
        String namespaceQuery = namespaceQueryTemplate
                .replace(KruizeConstants.KRUIZE_BULK_API.NAMESPACE_FILTERS, namespaceFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.LABELS, labels);

        String workloadQuery = workloadQueryTemplate
                .replace(KruizeConstants.KRUIZE_BULK_API.NAMESPACE_FILTERS, namespaceFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.CONTAINER_FILTERS, containerFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.WORKLOAD_FILTERS, workloadFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.LABELS, labels);

        String containerQuery = containerQueryTemplate
                .replace(KruizeConstants.KRUIZE_BULK_API.CONTAINER_FILTERS, containerFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.NAMESPACE_FILTERS, namespaceFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.WORKLOAD_FILTERS, workloadFilters)
                .replace(KruizeConstants.KRUIZE_BULK_API.LABELS, labels);

        String dataSourceName = dataSourceInfo.getName();

        namespaceQuery = namespaceQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));
        workloadQuery = workloadQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));
        containerQuery = containerQuery.replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

        // Replace unsupported workload types variable with actual filter (only for workload and container queries)
        String unsupportedWorkloadTypesFilter = AnalyzerConstants.getUnsupportedWorkloadTypesFilter();
        workloadQuery = workloadQuery.replace(AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE, unsupportedWorkloadTypesFilter);
        containerQuery = containerQuery.replace(AnalyzerConstants.UNSUPPORTED_WORKLOAD_TYPES_VARIABLE, unsupportedWorkloadTypesFilter);

        // Clean up label selector formatting (from filter chaining with leading commas)
        // Remove leading commas: {, filter} -> {filter}
        // Remove trailing commas: {filter, } -> {filter}
        // Normalize spacing: filter , filter -> filter, filter
        namespaceQuery = namespaceQuery.replaceAll("\\{,\\s*", "{").replaceAll(",\\s*\\}", "}").replaceAll("\\s+,", ",");
        workloadQuery = workloadQuery.replaceAll("\\{,\\s*", "{").replaceAll(",\\s*\\}", "}").replaceAll("\\s+,", ",");
        containerQuery = containerQuery.replaceAll("\\{,\\s*", "{").replaceAll(",\\s*\\}", "}").replaceAll("\\s+,", ",");

        LOGGER.info("namespaceQuery: {}", namespaceQuery);
        LOGGER.info("workloadQuery: {}", workloadQuery);
        LOGGER.info("containerQuery: {}", containerQuery);

        JsonArray namespacesDataResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
        LOGGER.debug("namespacesDataResultArray: {}", namespacesDataResultArray);
        if (!op.validateResultArray(namespacesDataResultArray)) {
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, null);
        } else {
            /**
             * Key: Name of namespace
             * Value: DataSourceNamespace object corresponding to a namespace
             */
            HashMap<String, DataSourceNamespace> datasourceNamespaces = dataSourceDetailsHelper.getActiveNamespaces(namespacesDataResultArray);
            LOGGER.debug("datasourceNamespaces: {}", datasourceNamespaces.keySet());
            dataSourceMetadataInfo = dataSourceDetailsHelper.createDataSourceMetadataInfoObject(dataSourceName, datasourceNamespaces);

            /**
             * Outer map:
             * Key: Name of namespace
             * <p>
             * Inner map:
             * Key: Name of workload
             * Value: DataSourceWorkload object matching the name
             * TODO -  get workload metadata for a given namespace
             */
            HashMap<String, HashMap<String, DataSourceWorkload>> datasourceWorkloads = new HashMap<>();
            JsonArray workloadDataResultArray = fetchQueryResults(dataSourceInfo, workloadQuery, startTime, endTime, steps);
            LOGGER.debug("workloadDataResultArray: {}", workloadDataResultArray);

            if (op.validateResultArray(workloadDataResultArray)) {
                datasourceWorkloads = dataSourceDetailsHelper.getWorkloadInfo(workloadDataResultArray);
            }
            if (hasLabelFilter && datasourceWorkloads.isEmpty()) {
                LOGGER.info("Label filter matched zero workloads — skipping experiment creation");
                // Clear singleton to avoid leaving partial state (namespaces populated but no workloads/containers)
                this.dataSourceMetadataInfo = null;
                return null;
            }
            dataSourceDetailsHelper.updateWorkloadDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads);

            /**
             * Outer map:
             * Key: Name of workload
             * <p>
             * Inner map:
             * Key: Name of container
             * Value: DataSourceContainer object matching the name
             * TODO - get container metadata for a given workload
             */
            HashMap<String, HashMap<String, DataSourceContainer>> datasourceContainers = new HashMap<>();
            JsonArray containerDataResultArray = fetchQueryResults(dataSourceInfo, containerQuery, startTime, endTime, steps);

            LOGGER.debug("containerDataResultArray: {}", containerDataResultArray);

            if (op.validateResultArray(containerDataResultArray)) {
                datasourceContainers = dataSourceDetailsHelper.getContainerInfo(containerDataResultArray);
            }
            dataSourceDetailsHelper.updateContainerDataSourceMetadataInfoObject(dataSourceName, dataSourceMetadataInfo,
                    datasourceWorkloads, datasourceContainers);
            return getDataSourceMetadataInfo(dataSourceInfo);
        }

        return null;

    }

    // Helper function to map fields to query templates
    private String getQueryTemplate(String field, MetadataProfile metadataProfile) {
        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();

        return switch (field) {
            case "namespace" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.NAMESPACE_METADATA_QUERY);
            case "workload" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.WORKLOAD_METADATA_QUERY);
            case "container" -> dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, AnalyzerConstants.CONTAINER_METADATA_QUERY);
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
     * Build NAMESPACE_FILTERS placeholder value with leading comma for chaining
     */
    private String buildNamespaceFilters(Map<String, String> includeResources, Map<String, String> excludeResources) {
        String includeRegex = includeResources.getOrDefault("namespaceRegex", "");
        String excludeRegex = excludeResources.getOrDefault("namespaceRegex", "");
        String filter = constructDynamicFilter("namespace", includeRegex, excludeRegex);
        return filter.isEmpty() ? "" : ", " + filter;
    }

    /**
     * Build WORKLOAD_FILTERS placeholder value (workload name filters only)
     */
    private String buildWorkloadFilters(Map<String, String> includeResources, Map<String, String> excludeResources) {
        String workloadIncludeRegex = includeResources.getOrDefault("workloadRegex", "");
        String workloadExcludeRegex = excludeResources.getOrDefault("workloadRegex", "");
        String workloadFilter = constructDynamicFilter("workload", workloadIncludeRegex, workloadExcludeRegex);
        return workloadFilter.isEmpty() ? "" : ", " + workloadFilter;
    }

    /**
     * Build CONTAINER_FILTERS placeholder value with leading comma for chaining
     */
    private String buildContainerFilters(Map<String, String> includeResources, Map<String, String> excludeResources) {
        String includeRegex = includeResources.getOrDefault("containerRegex", "");
        String excludeRegex = excludeResources.getOrDefault("containerRegex", "");
        String filter = constructDynamicFilter("container", includeRegex, excludeRegex);
        return filter.isEmpty() ? "" : ", " + filter;
    }

    /**
     * Build LABELS placeholder value (pod label filters)
     * Returns with leading comma for chaining
     */
    private String buildLabels(Map<String, String> includeResources, Map<String, String> excludeResources) {
        StringBuilder filters = new StringBuilder();

        // Add pod label filters
        String includePodLabelFilter = includeResources.getOrDefault("podLabelFilter", "");
        String excludePodLabelFilter = excludeResources.getOrDefault("podLabelFilter", "");

        if (!includePodLabelFilter.isEmpty()) {
            filters.append(includePodLabelFilter);
        }
        if (!excludePodLabelFilter.isEmpty()) {
            if (filters.length() > 0) filters.append(", ");
            filters.append(excludePodLabelFilter);
        }

        String result = filters.toString();
        return result.isEmpty() ? "" : ", " + result;
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
