package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Utility class for handling DataSourceDetailsInfo and related metadata.
 * This class provides methods to parse and organize information from JSON arrays
 * into appropriate data structures, facilitating the creation and updating of DataSourceDetailsInfo objects.
 */
public class DataSourceDetailsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsHelper.class);

    public DataSourceDetailsHelper() {
    }

    /**
     * Parses namespace information from a JsonArray and organizes
     * into a HashMap of namespaces
     *
     * @param resultArray The JsonArray containing the namespace information.
     * @return A HashMap<String, DataSourceNamespace> representing namespaces
     *
     * Example:
     * input resultArray structure:
     * {
     *   "result": [
     *     {
     *       "metric": {
     *         "namespace": "exampleNamespace"
     *       }
     *     },
     *     // ... additional result objects ...
     *   ]
     * }
     *
     * The function would parse the JsonObject and return a HashMap like:
     * {
     *   "exampleNamespace": {
     *     "namespaceName": "exampleNamespace"
     *   },
     *   // ... additional namespace entries ...
     * }
     */
    public HashMap<String, DataSourceNamespace> getActiveNamespaces(JsonArray resultArray) {
        HashMap<String, DataSourceNamespace> namespaceMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                if (result.isJsonObject()) {
                    JsonObject resultObject = result.getAsJsonObject();

                    // Check if the result object contains the "metric" field with "namespace"
                    if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                        JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                        // Extract the namespace value
                        if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                            String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();

                            DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespace,null);
                            namespaceMap.put(namespace, dataSourceNamespace);
                        }
                    }
                }
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.NAMESPACE_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == namespaceMap || namespaceMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceDetailsDebugMsgs.DEBUG_NAMESPACE_MAP_POPULATION);
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
     *         and their associated workload details.
     *
     * Example:
     * input resultArray structure:
     * {
     *   "result": [
     *     {
     *       "metric": {
     *         "namespace": "exampleNamespace",
     *         "workload": "exampleWorkload",
     *         "workloadType": "exampleWorkloadType"
     *       }
     *     },
     *     // ... additional result objects ...
     *   ]
     * }
     *
     * The function would parse the JsonObject and return a nested HashMap like:
     * {
     *   "exampleNamespace": {
     *     "exampleWorkload": {
     *       "workload": "exampleWorkload",
     *       "workloadType": "exampleWorkloadType"
     *     }
     *   },
     *   // ... additional namespace entries ...
     * }
     */
    public HashMap<String, HashMap<String, DataSourceWorkload>> getWorkloadInfo(JsonArray resultArray) {
        HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "namespace"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the namespace name
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                        String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();

                        // Check if the outer map already contains the namespace
                        if (!namespaceWorkloadMap.containsKey(namespace)) {
                            // If not, create a new inner hashmap for the namespace
                            namespaceWorkloadMap.put(namespace, new HashMap<>());
                        }

                        String workloadName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();
                        String workloadType = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD_TYPE).getAsString();
                        DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, workloadType,null);

                        // Put the DataSourceWorkload into the inner hashmap directly
                        namespaceWorkloadMap.get(namespace).put(workloadName, dataSourceWorkload);
                    }
                }
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.WORKLOAD_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == namespaceWorkloadMap || namespaceWorkloadMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceDetailsDebugMsgs.DEBUG_NAMESPACE_WORKLOAD_MAP_POPULATION);
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
     *         and their associated container details.
     *
     * Example:
     * input resultArray structure:
     * {
     *   "result": [
     *     {
     *       "metric": {
     *         "workload_name": "exampleWorkloadName",
     *         "container": "exampleContainer",
     *         "image_name": "exampleImageName"
     *       }
     *     },
     *     // ... additional result objects ...
     *   ]
     * }
     *
     * The function would parse the JsonObject and return a nested HashMap like:
     * {
     *   "exampleWorkloadName": {
     *     "exampleContainer": {
     *       "containerName": "exampleContainer",
     *       "containerImageName": "exampleImageName"
     *     }
     *   },
     *   // ... additional workload entries ...
     * }
     */
    public HashMap<String, HashMap<String, DataSourceContainer>> getContainerInfo(JsonArray resultArray) {
        HashMap<String, HashMap<String, DataSourceContainer>> workloadContainerMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "workload"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the workload name value
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD)) {
                        String workloadName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();

                        if (!workloadContainerMap.containsKey(workloadName)) {
                            workloadContainerMap.put(workloadName, new HashMap<>());
                        }

                        String containerName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME).getAsString();
                        String containerImageName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_IMAGE_NAME).getAsString();
                        DataSourceContainer dataSourceContainer = new DataSourceContainer(containerName, containerImageName);
                        workloadContainerMap.get(workloadName).put(containerName, dataSourceContainer);
                    }
                }
            }
        } catch (JsonParseException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.CONTAINER_JSON_PARSING_ERROR + e.getMessage());
        }

        if (null == workloadContainerMap || workloadContainerMap.isEmpty()) {
            LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceDetailsDebugMsgs.DEBUG_WORKLOAD_CONTAINER_MAP_POPULATION);
        }
        return workloadContainerMap;
    }

    /**
     * Creates and returns a DataSourceDetailsInfo object based on the provided parameters.
     * This function populates the DataSourceDetailsInfo object with information about active namespaces
     *
     * @param clusterGroupName       Name of the cluster group representing data source provider.
     * @param namespaceMap           Map of namespace objects
     * @return                       A DataSourceDetailsInfo object with populated information.
     */
    public DataSourceDetailsInfo createDataSourceDetailsInfoObject(String clusterGroupName, HashMap<String, DataSourceNamespace> namespaceMap) {

        try {
            DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version, null);

            DataSourceClusterGroup dataSourceClusterGroup = new DataSourceClusterGroup(clusterGroupName,null);

            DataSourceCluster dataSourceCluster = new DataSourceCluster(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME, namespaceMap);

            // Set cluster in cluster group
            HashMap<String, DataSourceCluster> clusters = new HashMap<>();
            clusters.put(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME, dataSourceCluster);
            dataSourceClusterGroup.setDataSourceClusterHashMap(clusters);

            // Set cluster group in DataSourceDetailsInfo
            HashMap<String, DataSourceClusterGroup> clusterGroups = new HashMap<>();
            clusterGroups.put(clusterGroupName, dataSourceClusterGroup);
            dataSourceDetailsInfo.setDataSourceClusterGroupHashMap(clusterGroups);

            return dataSourceDetailsInfo;
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.DATASOURCE_DETAILS_INFO_CREATION_ERROR + e.getMessage());
        }
        return null;
    }

    /**
     * Validates input parameters and retrieves the DataSourceCluster object.
     *
     * @param clusterGroupName      The name of the cluster group.
     * @param dataSourceDetailsInfo The DataSourceDetailsInfo object.
     * @param namespaceWorkloadMap  The map containing workload information.
     * @return The DataSourceCluster object if validation passes, or null if validation fails.
     */
    private DataSourceCluster validateInputParametersAndGetClusterObject(String clusterGroupName, DataSourceDetailsInfo dataSourceDetailsInfo, HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap) {

        if (null == clusterGroupName || clusterGroupName.isEmpty()) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_CLUSTER_GROUP_NAME);
            return null;
        }

        if (null == dataSourceDetailsInfo) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_INFO_OBJECT);
            return null;
        }

        if (null == namespaceWorkloadMap || namespaceWorkloadMap.isEmpty()) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_WORKLOAD_MAP);
            return null;
        }

        DataSourceClusterGroup dataSourceClusterGroup = dataSourceDetailsInfo.getDataSourceClusterGroupObject(clusterGroupName);

        if (dataSourceClusterGroup == null) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_CLUSTER_GROUP_OBJECT + clusterGroupName);
            return null;
        }

        return dataSourceClusterGroup.getDataSourceClusterObject(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME);
    }

    /**
     * Updates the workload metadata in the provided DataSourceDetailsInfo object for a specific cluster group.
     *
     * @param clusterGroupName      The name of the cluster group to update.
     * @param dataSourceDetailsInfo The DataSourceDetailsInfo object to update.
     * @param namespaceWorkloadMap  A map containing namespace as keys and workload data as values.
     */
    public void updateWorkloadDataSourceDetailsInfoObject(String clusterGroupName, DataSourceDetailsInfo dataSourceDetailsInfo,
                                                          HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap) {
        try {

            // Retrieve DataSourceCluster
            DataSourceCluster dataSourceCluster = validateInputParametersAndGetClusterObject(clusterGroupName, dataSourceDetailsInfo, namespaceWorkloadMap);

            // Update the DataSourceNamespaces using the provided map
            if (null != dataSourceCluster) {
                for (String namespace : namespaceWorkloadMap.keySet()) {
                    DataSourceNamespace dataSourceNamespace = dataSourceCluster.getDataSourceNamespaceObject(namespace);

                    if (null != dataSourceNamespace) {
                        // Bulk update workload data for the namespace
                        dataSourceNamespace.setDataSourceWorkloadHashMap(namespaceWorkloadMap.get(namespace));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.WORKLOAD_METADATA_UPDATE_ERROR+ e.getMessage());
        }
    }


    /**
     * Updates the container metadata in the provided DataSourceDetailsInfo object for a specific cluster group.
     *
     * @param clusterGroupName      The name of the cluster group to update.
     * @param dataSourceDetailsInfo The DataSourceDetailsInfo object to update.
     * @param namespaceWorkloadMap   A map containing namespace as keys and workload data as values.
     * @param workloadContainerMap  A map containing workload names as keys and container data as values.
     */
    public void updateContainerDataSourceDetailsInfoObject(String clusterGroupName, DataSourceDetailsInfo dataSourceDetailsInfo,
                                                           HashMap<String, HashMap<String, DataSourceWorkload>> namespaceWorkloadMap,
                                                           HashMap<String, HashMap<String, DataSourceContainer>> workloadContainerMap) {
        try {

            if (null == workloadContainerMap || workloadContainerMap.isEmpty()) {
                LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_CONTAINER_MAP);
                return;
            }
            // Retrieve DataSourceCluster
            DataSourceCluster dataSourceCluster = validateInputParametersAndGetClusterObject(clusterGroupName, dataSourceDetailsInfo, namespaceWorkloadMap);

            if (null != dataSourceCluster) {
                // Iterate over namespaces in namespaceWorkloadMap
                for (String namespace : namespaceWorkloadMap.keySet()) {
                    DataSourceNamespace dataSourceNamespace = dataSourceCluster.getDataSourceNamespaceObject(namespace);

                    if (null != dataSourceNamespace) {
                        // Iterate over workloads in workloadContainerMap
                        for (String workloadName : namespaceWorkloadMap.get(namespace).keySet()) {
                            DataSourceWorkload dataSourceWorkload = dataSourceNamespace.getDataSourceWorkloadObject(workloadName);

                            // Bulk update container data for the workload
                            if (null != dataSourceWorkload && workloadContainerMap.containsKey(workloadName)) {
                                dataSourceWorkload.setDataSourceContainerHashMap(workloadContainerMap.get(workloadName));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.CONTAINER_METADATA_UPDATE_ERROR + e.getMessage());
        }
    }
}


