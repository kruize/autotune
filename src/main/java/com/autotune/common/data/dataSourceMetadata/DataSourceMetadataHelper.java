package com.autotune.common.data.dataSourceMetadata;

import com.autotune.analyzer.metadataProfiles.MetadataProfile;
import com.autotune.common.data.metrics.Metric;
import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Utility class for handling DataSourceMetadataInfo and related metadata.
 * This class provides methods to parse and organize information from JSON arrays
 * into appropriate data structures, facilitating the creation and update of DataSourceMetadataInfo objects.
 */
public class DataSourceMetadataHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceMetadataHelper.class);

    public DataSourceMetadataHelper() {
    }

    /**
     * Parses namespace information from a JsonArray and organizes
     * into a HashMap of namespaces
     *
     * @param resultArray The JsonArray containing the namespace information.
     * @return A HashMap<String, DataSourceNamespace> representing namespaces
     * <p>
     * Example:
     * input resultArray structure:
     * {
     * "result": [
     * {
     * "metric": {
     * "namespace": "exampleNamespace"
     * }
     * },
     * // ... additional result objects ...
     * ]
     * }
     * <p>
     * The function would parse the JsonObject and return a HashMap like:
     * {
     * "exampleNamespace": {
     * "namespaceName": "exampleNamespace"
     * },
     * // ... additional namespace entries ...
     * }
     */
    public HashMap<String, DataSourceNamespace> getActiveNamespaces(JsonArray resultArray) {
        HashMap<String, DataSourceNamespace> namespaceMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {

                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "namespace"
                if (!resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) ||
                        !resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    continue;
                }
                JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                // Extract the namespace value
                if (!metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                    continue;
                }
                String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();

                DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespace, null);
                namespaceMap.put(namespace, dataSourceNamespace);
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == namespaceMap || namespaceMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_MAP_NOT_POPULATED);
        }

        return namespaceMap;
    }

    /**
     * Parses workload information from a JsonArray and organizes it into a nested map,
     * outer HashMap with namespaces as keys and
     * inner HashMap with workload name as keys and DataSourceWorkload objects as values.
     *
     * @param resultArray The JsonArray containing the workload information.
     * @return A HashMap<String, HashMap<String, DataSourceWorkload>> representing namespaces
     * and their associated workload details.
     * <p>
     * Example:
     * input resultArray structure:
     * {
     * "result": [
     * {
     * "metric": {
     * "namespace": "exampleNamespace",
     * "workload": "exampleWorkload",
     * "workloadType": "exampleWorkloadType"
     * }
     * },
     * // ... additional result objects ...
     * ]
     * }
     * <p>
     * The function would parse the JsonObject and return a nested HashMap like:
     * {
     * "exampleNamespace": {
     * "exampleWorkload": {
     * "workload": "exampleWorkload",
     * "workloadType": "exampleWorkloadType"
     * }
     * },
     * // ... additional namespace entries ...
     * }
     */
    public HashMap<String, HashMap<String, DataSourceWorkload>> getWorkloadInfo(JsonArray resultArray) {
        HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "namespace"
                if (!resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) ||
                        !resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    continue;
                }
                JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                // Extract the namespace name
                if (!metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                    continue;
                }
                String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();

                // Check if the outer map already contains the namespace
                if (!namespaceWorkloadMap.containsKey(namespace)) {
                    namespaceWorkloadMap.put(namespace, new HashMap<>());
                }

                String workloadName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();
                String workloadType = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD_TYPE).getAsString();
                DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, workloadType, null);

                // Put the DataSourceWorkload into the inner hashmap directly
                namespaceWorkloadMap.get(namespace).put(workloadName, dataSourceWorkload);
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.WORKLOAD_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == namespaceWorkloadMap || namespaceWorkloadMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_WORKLOAD_MAP_NOT_POPULATED);
        }
        return namespaceWorkloadMap;
    }

    /**
     * Parses container information from a JsonArray and organizes it into a nested map,
     * outer HashMap with workload names as keys and
     * inner HashMap with container name as keys and DataSourceWorkload objects as values.
     *
     * @param resultArray The JsonArray containing the container information.
     * @return A HashMap<String, HashMap<String, DataSourceContainer>> representing workloads
     * and their associated container details.
     * <p>
     * Example:
     * input resultArray structure:
     * {
     * "result": [
     * {
     * "metric": {
     * "workload_name": "exampleWorkloadName",
     * "container": "exampleContainer",
     * "image_name": "exampleImageName"
     * }
     * },
     * // ... additional result objects ...
     * ]
     * }
     * <p>
     * The function would parse the JsonObject and return a nested HashMap like:
     * {
     * "exampleWorkloadName": {
     * "exampleContainer": {
     * "containerName": "exampleContainer",
     * "containerImageName": "exampleImageName"
     * }
     * },
     * // ... additional workload entries ...
     * }
     */
    public HashMap<String, HashMap<String, DataSourceContainer>> getContainerInfo(JsonArray resultArray) {
        HashMap<String, HashMap<String, DataSourceContainer>> workloadContainerMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "workload"
                if (!resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) ||
                        !resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    continue;
                }
                JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                if (!metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD)) {
                    continue;
                }
                // Extract the workload name value
                String workloadName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();

                if (!workloadContainerMap.containsKey(workloadName)) {
                    workloadContainerMap.put(workloadName, new HashMap<>());
                }

                String containerName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME).getAsString();
                String containerImageName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_IMAGE_NAME).getAsString();
                DataSourceContainer dataSourceContainer = new DataSourceContainer(containerName, containerImageName);
                workloadContainerMap.get(workloadName).put(containerName, dataSourceContainer);
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.CONTAINER_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == workloadContainerMap || workloadContainerMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.WORKLOAD_CONTAINER_MAP_NOT_POPULATED);
        }
        return workloadContainerMap;
    }

    /**
     * Creates and returns a DataSourceMetadataInfo object based on the provided parameters.
     * This function populates the DataSourceMetadataInfo object with information about active namespaces
     *
     * @param dataSourceName Name of the data source.
     * @param namespaceMap   Map of namespace objects
     * @return A DataSourceMetadataInfo object with populated information.
     */
    public DataSourceMetadataInfo createDataSourceMetadataInfoObject(String dataSourceName, HashMap<String, DataSourceNamespace> namespaceMap) {

        try {
            DataSourceMetadataInfo dataSourceMetadataInfo = new DataSourceMetadataInfo(null);

            DataSource dataSource = new DataSource(dataSourceName, null);

            DataSourceCluster dataSourceCluster = new DataSourceCluster(KruizeConstants.DataSourceConstants.
                    DataSourceMetadataInfoConstants.CLUSTER_NAME, namespaceMap);

            // Set cluster in data source
            HashMap<String, DataSourceCluster> clusters = new HashMap<>();
            clusters.put(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.CLUSTER_NAME,
                    dataSourceCluster);
            dataSource.setClusters(clusters);

            // Set data source in DataSourceMetadataInfo
            HashMap<String, DataSource> dataSources = new HashMap<>();
            dataSources.put(dataSourceName, dataSource);
            dataSourceMetadataInfo.setDatasources(dataSources);

            return dataSourceMetadataInfo;
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_CREATION_ERROR + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the namespace metadata in the provided DataSourceMetadataInfo object for a specific data source.
     *
     * @param dataSourceName         The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceMap           A map containing namespace name as keys and namespace object as values.
     */
    public void updateNamespaceDataSourceMetadataInfoObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                                            HashMap<String, DataSourceNamespace> namespaceMap) {
        try {
            DataSourceCluster dataSourceCluster = dataSourceMetadataInfo.getDataSourceObject(dataSourceName)
                    .getDataSourceClusterObject("default");

            dataSourceCluster.getNamespaces().entrySet().removeIf(entry -> !namespaceMap.containsKey(entry.getKey()));

            //Add new namespaces, if not present
            for (HashMap.Entry<String, DataSourceNamespace> entry : namespaceMap.entrySet()) {
                String namespaceName = entry.getKey();
                DataSourceNamespace namespace = entry.getValue();
                if (!dataSourceCluster.getNamespaces().containsKey(namespaceName)) {
                    dataSourceCluster.getNamespaces().put(namespaceName, namespace);
                }
            }

        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_METADATA_UPDATE_ERROR + "{}", e.getMessage());
        }
    }

    /**
     * Validates input parameters and retrieves the DataSourceCluster object.
     *
     * @param dataSourceName         The name of the data source.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object.
     * @param namespaceWorkloadMap   The map containing workload information.
     * @return The DataSourceCluster object if validation passes, or null if validation fails.
     */
    private DataSourceCluster validateInputParametersAndGetClusterObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                                                         HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap) {

        if (null == dataSourceName || dataSourceName.isEmpty()) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.MISSING_DATASOURCE_METADATA_DATASOURCE_NAME);
            return null;
        }

        if (null == dataSourceMetadataInfo) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.MISSING_DATASOURCE_METADATA_INFO_OBJECT);
            return null;
        }

        if (null == namespaceWorkloadMap || namespaceWorkloadMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.MISSING_DATASOURCE_METADATA_WORKLOAD_MAP);
            return null;
        }

        DataSource dataSource = dataSourceMetadataInfo.getDataSourceObject(dataSourceName);

        if (null == dataSource) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.MISSING_DATASOURCE_METADATA_DATASOURCE_OBJECT + dataSourceName);
            return null;
        }

        return dataSource.getDataSourceClusterObject(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.CLUSTER_NAME);
    }

    /**
     * Updates the workload metadata in the provided DataSourceMetadataInfo object for a specific data source.
     *
     * @param dataSourceName         The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceWorkloadMap   A map containing namespace as keys and workload data as values.
     */
    public void updateWorkloadDataSourceMetadataInfoObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                                           HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap) {
        try {

            // Retrieve DataSourceCluster
            DataSourceCluster dataSourceCluster = validateInputParametersAndGetClusterObject(dataSourceName,
                    dataSourceMetadataInfo, namespaceWorkloadMap);

            if (null == dataSourceCluster) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_CLUSTER);
                return;
            }

            if (null == dataSourceCluster.getNamespaces() || dataSourceCluster.getNamespaces().isEmpty()) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_NAMESPACE_DATA);
                return;
            }

            // Update the DataSourceNamespaces using the provided map
            for (String namespace : namespaceWorkloadMap.keySet()) {
                DataSourceNamespace dataSourceNamespace = dataSourceCluster.getDataSourceNamespaceObject(namespace);

                if (null == dataSourceNamespace) {
                    continue;
                }
                // Bulk update workload data for the namespace
                dataSourceNamespace.setWorkloads(namespaceWorkloadMap.get(namespace));
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.WORKLOAD_METADATA_UPDATE_ERROR + e.getMessage());
        }
    }

    /**
     * Updates the container metadata in the provided DataSourceMetadataInfo object for a specific data source.
     *
     * @param dataSourceName         The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceWorkloadMap   A map containing namespace as keys and workload data as values.
     * @param workloadContainerMap   A map containing workload names as keys and container data as values.
     */
    public void updateContainerDataSourceMetadataInfoObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                                            HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap,
                                                            HashMap<String, HashMap<String, DataSourceContainer>> workloadContainerMap) {
        try {

            if (null == workloadContainerMap || workloadContainerMap.isEmpty()) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.MISSING_DATASOURCE_METADATA_CONTAINER_MAP);
                return;
            }
            // Retrieve DataSourceCluster
            DataSourceCluster dataSourceCluster = validateInputParametersAndGetClusterObject(dataSourceName,
                    dataSourceMetadataInfo, namespaceWorkloadMap);

            if (null == dataSourceCluster) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_CLUSTER);
                return;
            }

            if (null == dataSourceCluster.getNamespaces() || dataSourceCluster.getNamespaces().isEmpty()) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_NAMESPACE_DATA);
                return;
            }

            // Iterate over namespaces in namespaceWorkloadMap
            for (String namespace : namespaceWorkloadMap.keySet()) {
                DataSourceNamespace dataSourceNamespace = dataSourceCluster.getDataSourceNamespaceObject(namespace);

                // If namespace doesn't exist, create it (for workloads found via label filtering)
                if (null == dataSourceNamespace) {
                    LOGGER.info("Creating missing namespace '{}' for label-filtered workloads", namespace);
                    dataSourceNamespace = new DataSourceNamespace(namespace, new HashMap<>());
                    dataSourceCluster.getNamespaces().put(namespace, dataSourceNamespace);
                }

                // Iterate over workloads in namespaceWorkloadMap
                for (String workloadName : namespaceWorkloadMap.get(namespace).keySet()) {
                    DataSourceWorkload dataSourceWorkload = dataSourceNamespace.getDataSourceWorkloadObject(workloadName);

                    // If workload doesn't exist in namespace, add it (for workloads found via label filtering)
                    if (null == dataSourceWorkload) {
                        DataSourceWorkload matchedWorkload = namespaceWorkloadMap.get(namespace).get(workloadName);
                        if (matchedWorkload != null) {
                            LOGGER.info("Adding missing workload '{}' to namespace '{}'", workloadName, namespace);
                            dataSourceNamespace.getWorkloads().put(workloadName, matchedWorkload);
                            dataSourceWorkload = matchedWorkload;
                        } else {
                            continue;
                        }
                    }

                    // Bulk update container data for the workload
                    if (!workloadContainerMap.containsKey(workloadName)) {
                        continue;
                    }
                    dataSourceWorkload.setContainers(workloadContainerMap.get(workloadName));
                }
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.CONTAINER_METADATA_UPDATE_ERROR + e.getMessage());
        }
    }

    public void filterMetadataInfoObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                         HashMap<String, DataSourceNamespace> matchedNamespaces,
                                         HashMap<String, HashMap<String, DataSourceWorkload>> matchedWorkloads) {
        try {
            if (dataSourceMetadataInfo == null || dataSourceMetadataInfo.getDataSourceObject(dataSourceName) == null) {
                return;
            }

            DataSourceCluster cluster = dataSourceMetadataInfo.getDataSourceObject(dataSourceName)
                    .getDataSourceClusterObject(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.CLUSTER_NAME);

            if (cluster == null || cluster.getNamespaces() == null) {
                return;
            }

            // null = no filter requested, empty HashMap = filter requested but no matches
            boolean namespaceFilterRequested = matchedNamespaces != null;
            boolean workloadFilterRequested = matchedWorkloads != null;
            
            // If no filters were requested at all, don't filter anything
            if (!namespaceFilterRequested && !workloadFilterRequested) {
                LOGGER.debug("No label filters requested, keeping all workloads");
                return;
            }

            // Check if we have any matches
            boolean hasNamespaceMatches = namespaceFilterRequested && !matchedNamespaces.isEmpty();
            boolean hasWorkloadMatches = workloadFilterRequested && !matchedWorkloads.isEmpty();
            
            // OR logic: Keep results if EITHER filter has matches
            // If ALL requested filters returned no matches, skip filtering (keep all resources)
            // This prevents empty metadata when label filter queries fail or return no results
            if (namespaceFilterRequested && workloadFilterRequested) {
                // Both filters requested - if BOTH have no matches, skip filtering
                if (!hasNamespaceMatches && !hasWorkloadMatches) {
                    LOGGER.warn("Both label filters requested but no resources matched - skipping label filtering (keeping all workloads). " +
                               "This may indicate label filter queries are not finding matches. Check that kube_namespace_labels and kube_pod_labels metrics exist.");
                    return;
                }
                // If at least one has matches, continue with filtering below
                LOGGER.info("Label filters: namespace matches={}, workload matches={}", hasNamespaceMatches, hasWorkloadMatches);
            } else if (namespaceFilterRequested && !hasNamespaceMatches) {
                // Only namespace filter requested, no matches - skip filtering
                LOGGER.warn("Namespace label filter requested but no resources matched - skipping label filtering (keeping all workloads). " +
                           "Check that kube_namespace_labels metric exists and has the requested labels.");
                return;
            } else if (workloadFilterRequested && !hasWorkloadMatches) {
                // Only workload filter requested, no matches - skip filtering
                LOGGER.warn("Workload label filter requested but no resources matched - skipping label filtering (keeping all workloads). " +
                           "Check that kube_pod_labels metric exists and has the requested labels.");
                return;
            }

            cluster.getNamespaces().entrySet().removeIf(namespaceEntry -> {
                String namespaceName = namespaceEntry.getKey();
                DataSourceNamespace namespace = namespaceEntry.getValue();

                boolean namespaceMatched = hasNamespaceMatches && matchedNamespaces.containsKey(namespaceName);
                HashMap<String, DataSourceWorkload> namespaceMatchedWorkloads =
                        hasWorkloadMatches ? matchedWorkloads.get(namespaceName) : null;

                // If namespace matched by namespace filter, keep it
                if (namespaceMatched) {
                    return false;
                }

                // If workload filter has matches for this namespace, filter workloads and keep namespace
                if (namespaceMatchedWorkloads != null && !namespaceMatchedWorkloads.isEmpty()) {
                    if (namespace.getWorkloads() != null) {
                        namespace.getWorkloads().entrySet().removeIf(workloadEntry ->
                                !namespaceMatchedWorkloads.containsKey(workloadEntry.getKey()));
                    }
                    // Keep namespace if it still has workloads after filtering
                    return namespace.getWorkloads() == null || namespace.getWorkloads().isEmpty();
                }

                // No matches from either filter for this namespace - remove it
                return true;
            });
        } catch (Exception e) {
            LOGGER.error("Error while filtering datasource metadata info {}", e.getMessage());
        }
    }

    public String getQueryFromProfile(MetadataProfile metadataProfile, String metricName) {
        List<Metric> metrics = metadataProfile.getQueryVariables();
        LOGGER.info("Looking for metric '{}' in profile with {} metrics", metricName, metrics != null ? metrics.size() : 0);
        
        if (metrics == null || metrics.isEmpty()) {
            LOGGER.warn("No metrics found in metadata profile");
            return null;
        }
        
        // Log all available metric names for debugging
        LOGGER.info("Available metrics in profile: {}",
            metrics.stream().map(Metric::getName).collect(java.util.stream.Collectors.joining(", ")));
        
        for (Metric metric : metrics) {
            String name = metric.getName();
            LOGGER.debug("Checking metric: {}", name);
            if (name.contains(metricName)) {
                LOGGER.info("Found matching metric: {}", name);
                if (metric.getAggregationFunctionsMap() == null) {
                    LOGGER.warn("Metric '{}' has null aggregation functions map", name);
                    return null;
                }
                if (!metric.getAggregationFunctionsMap().containsKey(KruizeConstants.JSONKeys.SUM)) {
                    LOGGER.warn("Metric '{}' does not have 'sum' aggregation function. Available functions: {}",
                               name, metric.getAggregationFunctionsMap().keySet());
                    return null;
                }
                String query = metric.getAggregationFunctionsMap().get(KruizeConstants.JSONKeys.SUM).getQuery();
                LOGGER.info("Retrieved query for metric '{}': {}", name, query);
                return query;
            }
        }
        LOGGER.warn("Metric '{}' not found in profile. Available metrics: {}",
            metricName, metrics.stream().map(Metric::getName).collect(java.util.stream.Collectors.joining(", ")));
        return null;
    }
}
