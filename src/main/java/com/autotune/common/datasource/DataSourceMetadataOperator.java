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

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.analyzer.metadataProfiles.MetadataProfileCollection;
import com.autotune.analyzer.utils.AnalyzerConstants;
import static com.autotune.analyzer.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import com.autotune.common.data.dataSourceMetadata.DataSource;
import com.autotune.common.data.dataSourceMetadata.DataSourceCluster;
import com.autotune.common.data.dataSourceMetadata.DataSourceContainer;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataHelper;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.data.dataSourceMetadata.DataSourceNamespace;
import com.autotune.common.data.dataSourceMetadata.DataSourceWorkload;
import com.autotune.utils.GenericRestApiClient;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
        // However, for exclude-only filters, skip this step and run main queries first
        LOGGER.info("Applying label filters first to identify matching resources...");
        LOGGER.info("Include resources: {}", includeResources);
        LOGGER.info("Exclude resources: {}", excludeResources);
        
        HashMap<String, DataSourceNamespace> matchedNamespaces = getNamespacesMatchingLabelFilter(dataSourceInfo, metadataProfile, includeResources, excludeResources, startTime, endTime, steps, measurementDuration, namespaceQuery, false);
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = getWorkloadsMatchingPodLabelFilter(dataSourceInfo, metadataProfile, includeResources, excludeResources, startTime, endTime, steps, measurementDuration, workloadQuery, false);
        
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

            // For exclude-only label filters, we need to query for resources to exclude now
            // (after main queries have run) and populate matchedNamespaces/matchedWorkloads
            if (matchedNamespaces.isEmpty() && matchedWorkloads.isEmpty()) {
                String excludeNamespaceLabelFilter = excludeResources.getOrDefault("namespaceLabelFilter", "");
                String excludePodLabelFilter = excludeResources.getOrDefault("podLabelFilter", "");
                
                if (!excludeNamespaceLabelFilter.isEmpty() || !excludePodLabelFilter.isEmpty()) {
                    LOGGER.info("Exclude-only filters detected - querying for resources to exclude after main queries");
                    // Get all namespaces/workloads from the metadata that was just fetched
                    DataSourceCluster cluster = dataSourceMetadataInfo.getDataSourceObject(dataSourceName)
                            .getDataSourceClusterObject(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.CLUSTER_NAME);
                    
                    if (cluster != null && cluster.getNamespaces() != null) {
                        HashMap<String, DataSourceNamespace> allNamespaces = new HashMap<>(cluster.getNamespaces());
                        HashMap<String, HashMap<String, DataSourceWorkload>> allWorkloads = new HashMap<>();
                        allNamespaces.forEach((ns, namespace) -> {
                            if (namespace.getWorkloads() != null) {
                                allWorkloads.put(ns, new HashMap<>(namespace.getWorkloads()));
                            }
                        });
                        
                        // Now query for resources to exclude and remove them from all resources
                        matchedNamespaces.putAll(allNamespaces);
                        allWorkloads.forEach((ns, wls) -> {
                            matchedWorkloads.computeIfAbsent(ns, k -> new HashMap<>()).putAll(wls);
                        });
                        
                        // Query for resources to exclude
                        // Pass exclude filters as INCLUDE filters to actually query for them
                        Map<String, String> excludeAsInclude = new HashMap<>();
                        if (!excludeNamespaceLabelFilter.isEmpty()) {
                            excludeAsInclude.put("namespaceLabelFilter", excludeNamespaceLabelFilter);
                        }
                        if (!excludePodLabelFilter.isEmpty()) {
                            excludeAsInclude.put("podLabelFilter", excludePodLabelFilter);
                        }
                        
                        HashMap<String, DataSourceNamespace> excludedNamespaces = getNamespacesMatchingLabelFilter(dataSourceInfo, metadataProfile, excludeAsInclude, new HashMap<>(), startTime, endTime, steps, measurementDuration, namespaceQuery, true);
                        HashMap<String, HashMap<String, DataSourceWorkload>> excludedWorkloads = getWorkloadsMatchingPodLabelFilter(dataSourceInfo, metadataProfile, excludeAsInclude, new HashMap<>(), startTime, endTime, steps, measurementDuration, workloadQuery, true);
                        
                        LOGGER.info("Queried for resources to exclude - Namespaces: {}, Workloads: {}",
                                   excludedNamespaces.size(),
                                   excludedWorkloads.values().stream().mapToInt(Map::size).sum());
                        
                        // Remove excluded resources
                        LOGGER.info("Removing excluded namespaces: {}", excludedNamespaces.keySet());
                        excludedNamespaces.keySet().forEach(ns -> {
                            LOGGER.info("EXCLUDED namespace: {}", ns);
                            matchedNamespaces.remove(ns);
                        });
                        
                        excludedWorkloads.forEach((ns, wls) -> {
                            if (matchedWorkloads.containsKey(ns)) {
                                LOGGER.info("Namespace {} has {} workloads to exclude: {}", ns, wls.size(), wls.keySet());
                                wls.keySet().forEach(wl -> {
                                    LOGGER.info("EXCLUDED workload: {} in namespace {}", wl, ns);
                                    matchedWorkloads.get(ns).remove(wl);
                                });
                            } else {
                                LOGGER.warn("Namespace {} not found in matchedWorkloads, cannot exclude workloads: {}", ns, wls.keySet());
                            }
                        });
                        
                        LOGGER.info("After exclusions - Matched namespaces: {}, Matched workloads: {}",
                                   matchedNamespaces.size(),
                                   matchedWorkloads.values().stream().mapToInt(Map::size).sum());
                    }
                }
            }
            
            // Apply the label filter results to remove non-matching resources
            LOGGER.info("Calling filterMetadataInfoObject with matchedNamespaces: {}, matchedWorkloads: {}",
                       matchedNamespaces.keySet(),
                       matchedWorkloads.entrySet().stream()
                           .collect(java.util.stream.Collectors.toMap(
                               java.util.Map.Entry::getKey,
                               e -> e.getValue().keySet()
                           )));
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
     * @param includeResources Map containing the namespaceLabelFilter for inclusion
     * @param excludeResources Map containing the namespaceLabelFilter for exclusion
     * @param startTime Start time for the query
     * @param endTime End time for the query
     * @param steps Query step interval
     * @param measurementDuration Measurement duration in minutes
     * @return HashMap of namespaces matching the include filter and not matching the exclude filter
     */
    private HashMap<String, DataSourceNamespace> getNamespacesMatchingLabelFilter(DataSourceInfo dataSourceInfo, MetadataProfile metadataProfile,
                                                                                   Map<String, String> includeResources, Map<String, String> excludeResources,
                                                                                   long startTime, long endTime, int steps, int measurementDuration, String namespaceQueryForAll, boolean forceQuery) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, DataSourceNamespace> matchedNamespaces = new HashMap<>();
        String namespaceLabelFilter = includeResources.getOrDefault("namespaceLabelFilter", "");
        String excludeNamespaceLabelFilter = excludeResources.getOrDefault("namespaceLabelFilter", "");

        LOGGER.info("Namespace label filtering - Include filter: '{}', Exclude filter: '{}'",
                   namespaceLabelFilter, excludeNamespaceLabelFilter);

        if (namespaceLabelFilter.isEmpty() && excludeNamespaceLabelFilter.isEmpty()) {
            LOGGER.info("No namespace label filters provided, skipping namespace label filtering");
            return matchedNamespaces;
        }

        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        String queryTemplate = dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, "namespacesWithLabelFilter");
        
        if (queryTemplate == null) {
            LOGGER.warn("namespacesWithLabelFilter query not found in metadata profile, skipping namespace label filtering");
            return matchedNamespaces;
        }

        // For exclude-only filters (no include filters), skip UNLESS we're being called to query FOR exclusion (forceQuery=true)
        if (!forceQuery) {
            boolean hasOtherIncludeFilters = !includeResources.getOrDefault("namespaceRegex", "").isEmpty() ||
                                             !includeResources.getOrDefault("workloadRegex", "").isEmpty() ||
                                             !includeResources.getOrDefault("containerRegex", "").isEmpty();
            
            if (namespaceLabelFilter.isEmpty() && !excludeNamespaceLabelFilter.isEmpty() && !hasOtherIncludeFilters) {
                LOGGER.info("Only exclude label filter provided (no other include filters) - skipping label filter queries, will apply exclusions after main queries");
                return matchedNamespaces;
            }
        }

        // Process include filters (OR logic)
        if (!namespaceLabelFilter.isEmpty()) {
            String[] labelFilters = namespaceLabelFilter.split(",");
            LOGGER.info("Include namespaceLabelFilter has {} label(s): {}", labelFilters.length, namespaceLabelFilter);
            
            for (int i = 0; i < labelFilters.length; i++) {
                String singleLabelFilter = labelFilters[i].trim();
                String namespaceQuery = queryTemplate
                        .replace("LABEL_FILTER", singleLabelFilter)
                        .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

                LOGGER.info("Executing include namespace label filter query {} of {}: {}", i+1, labelFilters.length, namespaceQuery);
                JsonArray namespaceQueryResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
                LOGGER.info("Query {} returned {} results", i+1, namespaceQueryResultArray != null ? namespaceQueryResultArray.size() : 0);
                
                HashMap<String, DataSourceNamespace> currentMatches = dataSourceDetailsHelper.getActiveNamespaces(namespaceQueryResultArray);
                LOGGER.info("Query {} matched {} namespace(s): {}", i+1, currentMatches.size(), currentMatches.keySet());
                
                // Merge results (OR logic - add all matches from each label)
                matchedNamespaces.putAll(currentMatches);
            }
            
            LOGGER.info("Total included namespaces after OR logic: {} - {}", matchedNamespaces.size(), matchedNamespaces.keySet());
        }

        // Process exclude filters (remove matching namespaces)
        if (!excludeNamespaceLabelFilter.isEmpty()) {
            LOGGER.info("=== PROCESSING EXCLUDE NAMESPACE FILTERS ===");
            HashMap<String, DataSourceNamespace> excludedNamespaces = new HashMap<>();
            String[] excludeLabelFilters = excludeNamespaceLabelFilter.split(",");
            LOGGER.info("Exclude namespaceLabelFilter has {} label(s): {}", excludeLabelFilters.length, excludeNamespaceLabelFilter);
            
            for (int i = 0; i < excludeLabelFilters.length; i++) {
                String singleLabelFilter = excludeLabelFilters[i].trim();
                String namespaceQuery = queryTemplate
                        .replace("LABEL_FILTER", singleLabelFilter)
                        .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

                LOGGER.info("Executing exclude namespace label filter query {} of {}: {}", i+1, excludeLabelFilters.length, namespaceQuery);
                JsonArray namespaceQueryResultArray = fetchQueryResults(dataSourceInfo, namespaceQuery, startTime, endTime, steps);
                LOGGER.info("Query {} returned {} results", i+1, namespaceQueryResultArray != null ? namespaceQueryResultArray.size() : 0);
                
                HashMap<String, DataSourceNamespace> currentExcludes = dataSourceDetailsHelper.getActiveNamespaces(namespaceQueryResultArray);
                LOGGER.info("Query {} matched {} namespace(s) to exclude: {}", i+1, currentExcludes.size(), currentExcludes.keySet());
                
                // Collect all namespaces to exclude (OR logic)
                excludedNamespaces.putAll(currentExcludes);
            }
            
            LOGGER.info("Total namespaces to exclude: {} - {}", excludedNamespaces.size(), excludedNamespaces.keySet());
            
            // Remove excluded namespaces from matched namespaces
            int beforeExclusion = matchedNamespaces.size();
            excludedNamespaces.keySet().forEach(ns -> {
                if (matchedNamespaces.containsKey(ns)) {
                    matchedNamespaces.remove(ns);
                    LOGGER.info("EXCLUDED namespace: {}", ns);
                } else {
                    LOGGER.debug("Namespace {} was already not in matched set", ns);
                }
            });
            int afterExclusion = matchedNamespaces.size();
            LOGGER.info("Namespace exclusion complete - Before: {}, After: {}, Excluded: {}",
                       beforeExclusion, afterExclusion, (beforeExclusion - afterExclusion));
            LOGGER.info("Final matched namespaces after exclusion: {} - {}", matchedNamespaces.size(), matchedNamespaces.keySet());
        }
        
        return matchedNamespaces;
    }

    /**
     * Fetches workloads whose pods match the specified pod label filter.
     * Uses the workloadsWithPodLabelFilter query from the metadata profile.
     *
     * @param dataSourceInfo The data source information
     * @param metadataProfile The metadata profile containing the query
     * @param includeResources Map containing the podLabelFilter for inclusion
     * @param excludeResources Map containing the podLabelFilter for exclusion
     * @param startTime Start time for the query
     * @param endTime End time for the query
     * @param steps Query step interval
     * @param measurementDuration Measurement duration in minutes
     * @return HashMap of workloads whose pods match the include filter and not matching the exclude filter
     */
    private HashMap<String, HashMap<String, DataSourceWorkload>> getWorkloadsMatchingPodLabelFilter(DataSourceInfo dataSourceInfo, MetadataProfile metadataProfile,
                                                                                                     Map<String, String> includeResources, Map<String, String> excludeResources,
                                                                                                     long startTime, long endTime, int steps, int measurementDuration, String workloadQueryForAll, boolean forceQuery) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads = new HashMap<>();
        String podLabelFilter = includeResources.getOrDefault("podLabelFilter", "");
        String excludePodLabelFilter = excludeResources.getOrDefault("podLabelFilter", "");

        LOGGER.info("Pod label filtering - Include filter: '{}', Exclude filter: '{}'",
                   podLabelFilter, excludePodLabelFilter);

        if (podLabelFilter.isEmpty() && excludePodLabelFilter.isEmpty()) {
            LOGGER.info("No pod label filters provided, skipping pod label filtering");
            return matchedWorkloads;
        }

        DataSourceMetadataHelper dataSourceDetailsHelper = new DataSourceMetadataHelper();
        String queryTemplate = dataSourceDetailsHelper.getQueryFromProfile(metadataProfile, "workloadsWithPodLabelFilter");
        
        if (queryTemplate == null) {
            LOGGER.warn("workloadsWithPodLabelFilter query not found in metadata profile");
            // If query is not found in profile, the filter will be skipped
            return matchedWorkloads;
            
        }

        // For exclude-only filters (no include filters), skip UNLESS we're being called to query FOR exclusion (forceQuery=true)
        if (!forceQuery) {
            boolean hasOtherIncludeFilters = !includeResources.getOrDefault("namespaceRegex", "").isEmpty() ||
                                             !includeResources.getOrDefault("workloadRegex", "").isEmpty() ||
                                             !includeResources.getOrDefault("containerRegex", "").isEmpty();
            
            if (podLabelFilter.isEmpty() && !excludePodLabelFilter.isEmpty() && !hasOtherIncludeFilters) {
                LOGGER.info("Only exclude label filter provided (no other include filters) - skipping label filter queries, will apply exclusions after main queries");
                return matchedWorkloads;
            }
        }

        // Process include filters (OR logic)
        if (!podLabelFilter.isEmpty()) {
            String[] labelFilters = podLabelFilter.split(",");
            LOGGER.info("Include podLabelFilter has {} label(s): {}", labelFilters.length, podLabelFilter);
            
            for (int i = 0; i < labelFilters.length; i++) {
                String singleLabelFilter = labelFilters[i].trim();
                String workloadQuery = queryTemplate
                        .replace("LABEL_FILTER", singleLabelFilter)
                        .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

                LOGGER.info("Executing include pod label filter query {} of {}: {}", i+1, labelFilters.length, workloadQuery);
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
            LOGGER.info("Total included workloads after OR logic: {}", totalWorkloads);
            matchedWorkloads.forEach((ns, wls) -> {
                LOGGER.info("  Namespace {}: {} workload(s) - {}", ns, wls.size(), wls.keySet());
            });
        }

        // Process exclude filters (remove matching workloads)
        if (!excludePodLabelFilter.isEmpty()) {
            LOGGER.info("=== PROCESSING EXCLUDE POD LABEL FILTERS ===");
            HashMap<String, HashMap<String, DataSourceWorkload>> excludedWorkloads = new HashMap<>();
            String[] excludeLabelFilters = excludePodLabelFilter.split(",");
            LOGGER.info("Exclude podLabelFilter has {} label(s): {}", excludeLabelFilters.length, excludePodLabelFilter);
            
            for (int i = 0; i < excludeLabelFilters.length; i++) {
                String singleLabelFilter = excludeLabelFilters[i].trim();
                String workloadQuery = queryTemplate
                        .replace("LABEL_FILTER", singleLabelFilter)
                        .replace(AnalyzerConstants.MEASUREMENT_DURATION_IN_MIN_VARAIBLE, Integer.toString(measurementDuration));

                LOGGER.info("Executing exclude pod label filter query {} of {}: {}", i+1, excludeLabelFilters.length, workloadQuery);
                JsonArray workloadQueryResultArray = fetchQueryResults(dataSourceInfo, workloadQuery, startTime, endTime, steps);
                LOGGER.info("Query {} returned {} results", i+1, workloadQueryResultArray != null ? workloadQueryResultArray.size() : 0);
                
                HashMap<String, HashMap<String, DataSourceWorkload>> currentExcludes = dataSourceDetailsHelper.getWorkloadInfo(workloadQueryResultArray);
                int currentWorkloadCount = currentExcludes.values().stream().mapToInt(Map::size).sum();
                LOGGER.info("Query {} matched {} workload(s) to exclude", i+1, currentWorkloadCount);
                
                // Collect all workloads to exclude (OR logic)
                currentExcludes.forEach((namespace, workloads) -> {
                    LOGGER.debug("Collecting {} workload(s) to exclude from namespace {}: {}", workloads.size(), namespace, workloads.keySet());
                    excludedWorkloads.computeIfAbsent(namespace, k -> new HashMap<>()).putAll(workloads);
                });
            }
            
            int totalExcludedWorkloads = excludedWorkloads.values().stream().mapToInt(Map::size).sum();
            LOGGER.info("Total workloads to exclude: {}", totalExcludedWorkloads);
            excludedWorkloads.forEach((ns, wls) -> {
                LOGGER.info("  Namespace {}: {} workload(s) to exclude - {}", ns, wls.size(), wls.keySet());
            });
            
            // Remove excluded workloads from matched workloads
            int beforeExclusion = matchedWorkloads.values().stream().mapToInt(Map::size).sum();
            excludedWorkloads.forEach((namespace, workloads) -> {
                if (matchedWorkloads.containsKey(namespace)) {
                    workloads.keySet().forEach(workloadName -> {
                        if (matchedWorkloads.get(namespace).containsKey(workloadName)) {
                            matchedWorkloads.get(namespace).remove(workloadName);
                            LOGGER.info("EXCLUDED workload: {} from namespace: {}", workloadName, namespace);
                        }
                    });
                    // Remove namespace if no workloads left
                    if (matchedWorkloads.get(namespace).isEmpty()) {
                        matchedWorkloads.remove(namespace);
                        LOGGER.info("Removed empty namespace {} after workload exclusion", namespace);
                    }
                } else {
                    LOGGER.debug("Namespace {} not in matched workloads, skipping exclusion", namespace);
                }
            });
            
            int afterExclusion = matchedWorkloads.values().stream().mapToInt(Map::size).sum();
            LOGGER.info("Workload exclusion complete - Before: {}, After: {}, Excluded: {}",
                       beforeExclusion, afterExclusion, (beforeExclusion - afterExclusion));
            int finalWorkloads = matchedWorkloads.values().stream().mapToInt(Map::size).sum();
            LOGGER.info("Final matched workloads after exclusion: {}", finalWorkloads);
            matchedWorkloads.forEach((ns, wls) -> {
                LOGGER.info("  Namespace {}: {} workload(s) - {}", ns, wls.size(), wls.keySet());
            });
        }
        
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
