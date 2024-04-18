package com.autotune.common.data.dataSourceMetadata;

import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

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

                DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespace,null);
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
                DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, workloadType,null);

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
     * @param dataSourceName         Name of the data source.
     * @param namespaceMap           Map of namespace objects
     * @return                       A DataSourceMetadataInfo object with populated information.
     */
    public DataSourceMetadataInfo createDataSourceMetadataInfoObject(String dataSourceName, HashMap<String, DataSourceNamespace> namespaceMap) {

        try {
            DataSourceMetadataInfo dataSourceMetadataInfo = new DataSourceMetadataInfo(null);

            DataSource dataSource = new DataSource(dataSourceName,null);

            DataSourceCluster dataSourceCluster = new DataSourceCluster(KruizeConstants.DataSourceConstants.
                    DataSourceMetadataInfoConstants.CLUSTER_NAME, namespaceMap);

            // Set cluster in data source
            HashMap<String, DataSourceCluster> clusters = new HashMap<>();
            clusters.put(KruizeConstants.DataSourceConstants.DataSourceMetadataInfoConstants.CLUSTER_NAME,
                    dataSourceCluster);
            dataSource.setDataSourceClusterHashMap(clusters);

            // Set data source in DataSourceMetadataInfo
            HashMap<String, DataSource> dataSources = new HashMap<>();
            dataSources.put(dataSourceName, dataSource);
            dataSourceMetadataInfo.setDataSourceHashMap(dataSources);

            return dataSourceMetadataInfo;
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.DATASOURCE_METADATA_INFO_CREATION_ERROR + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the namespace metadata in the provided DataSourceMetadataInfo object for a specific data source.
     *
     * @param dataSourceName        The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceMap          A map containing namespace name as keys and namespace object as values.
     */
    public void updateNamespaceDataSourceMetadataInfoObject(String dataSourceName, DataSourceMetadataInfo dataSourceMetadataInfo,
                                                            HashMap<String, DataSourceNamespace> namespaceMap) {
        try {
            DataSourceCluster dataSourceCluster = dataSourceMetadataInfo.getDataSourceObject(dataSourceName)
                    .getDataSourceClusterObject("default");

            dataSourceCluster.getDataSourceNamespaceHashMap().entrySet().removeIf(entry -> !namespaceMap.containsKey(entry.getKey()));

            //Add new namespaces, if not present
            for (HashMap.Entry<String, DataSourceNamespace> entry : namespaceMap.entrySet()) {
                String namespaceName = entry.getKey();
                DataSourceNamespace namespace = entry.getValue();
                if (!dataSourceCluster.getDataSourceNamespaceHashMap().containsKey(namespaceName)) {
                    dataSourceCluster.getDataSourceNamespaceHashMap().put(namespaceName, namespace);
                }
            }

        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.NAMESPACE_METADATA_UPDATE_ERROR + "{}", e.getMessage());
        }
    }

    /**
     * Validates input parameters and retrieves the DataSourceCluster object.
     *
     * @param dataSourceName      The name of the data source.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object.
     * @param namespaceWorkloadMap  The map containing workload information.
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
     * @param dataSourceName        The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceWorkloadMap  A map containing namespace as keys and workload data as values.
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

            if (null == dataSourceCluster.getDataSourceNamespaceHashMap() || dataSourceCluster.getDataSourceNamespaceHashMap().isEmpty()) {
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
                dataSourceNamespace.setDataSourceWorkloadHashMap(namespaceWorkloadMap.get(namespace));
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.WORKLOAD_METADATA_UPDATE_ERROR+ e.getMessage());
        }
    }
    /**
     * Updates the container metadata in the provided DataSourceMetadataInfo object for a specific data source.
     *
     * @param dataSourceName         The name of the data source to update.
     * @param dataSourceMetadataInfo The DataSourceMetadataInfo object to update.
     * @param namespaceWorkloadMap   A map containing namespace as keys and workload data as values.
     * @param workloadContainerMap  A map containing workload names as keys and container data as values.
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

            if (null == dataSourceCluster.getDataSourceNamespaceHashMap() || dataSourceCluster.getDataSourceNamespaceHashMap().isEmpty()) {
                LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_NAMESPACE_DATA);
                return;
            }

            // Iterate over namespaces in namespaceWorkloadMap
            for (String namespace : namespaceWorkloadMap.keySet()) {
                DataSourceNamespace dataSourceNamespace = dataSourceCluster.getDataSourceNamespaceObject(namespace);

                if (null == dataSourceNamespace) {
                    LOGGER.debug(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.INVALID_DATASOURCE_METADATA_NAMESPACE);
                    return;
                }

                // Iterate over workloads in namespaceWorkloadMap
                for (String workloadName : namespaceWorkloadMap.get(namespace).keySet()) {
                    DataSourceWorkload dataSourceWorkload = dataSourceNamespace.getDataSourceWorkloadObject(workloadName);

                    // Bulk update container data for the workload
                    if (null == dataSourceWorkload || !workloadContainerMap.containsKey(workloadName)) {
                        continue;
                    }
                    dataSourceWorkload.setDataSourceContainerHashMap(workloadContainerMap.get(workloadName));
                }
            }
        } catch (Exception e) {
            LOGGER.error(KruizeConstants.DataSourceConstants.DataSourceMetadataErrorMsgs.CONTAINER_METADATA_UPDATE_ERROR + e.getMessage());
        }
    }
}
