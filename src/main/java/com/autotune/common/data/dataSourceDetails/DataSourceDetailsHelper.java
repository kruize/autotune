package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class DataSourceDetailsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsHelper.class);

    /**
     * Parses namespace information from a JsonArray and organizes
     * into a List of namespaces
     *
     * @param resultArray The JsonArray containing the namespace information.
     * @return A List<String> representing namespaces
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
     * output List:
     * ["exampleNamespace", ... additional namespaces ...]
     */
    public List<String> getActiveNamespaces(JsonArray resultArray) {
        List<String> namespaces = new ArrayList<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                if (result.isJsonObject()) {
                    JsonObject resultObject = result.getAsJsonObject();

                    // Check if the result object contains the "metric" field with "namespace"
                    if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                        JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                        // Extract the namespace value and add it to the list
                        if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                            String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();
                            namespaces.add(namespace);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON: " + e.getMessage());
        }
        return namespaces;
    }

    /**
     * Parses workload information from a JsonArray and organizes it into a HashMap
     * with namespaces as keys and lists of DataSourceWorkload objects as values.
     *
     * @param resultArray The JsonArray containing the workload information.
     * @return A HashMap<String, List<DataSourceWorkload>> representing namespaces
     *         and their associated workload details.
     *
     * Example:
     * input dataObject structure:
     * {
     *   "result": [
     *     {
     *       "metric": {
     *         "namespace": "exampleNamespace",
     *         "workload": "exampleWorkload",
     *         "workload_type": "exampleType"
     *       }
     *     },
     *     // ... additional result objects ...
     *   ]
     * }
     *
     * The function would parse the JsonObject and return a HashMap like:
     * {
     *   "exampleNamespace": [
     *     {
     *       "workload_name": "exampleWorkload",
     *       "workload_type": "exampleType",
     *       "containers": null // Assuming containers are not included in this function
     *     },
     *     // ... additional DataSourceWorkload objects ...
     *   ],
     *   // ... additional namespaces ...
     * }
     */
    public HashMap<String, List<DataSourceWorkload>> getWorkloadInfo(JsonArray resultArray) {
        HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "namespace"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the namespace value
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE)) {
                        String namespace = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.NAMESPACE).getAsString();

                        // Create Workload object and populate
                        DataSourceWorkload dataSourceWorkload = new DataSourceWorkload();
                        dataSourceWorkload.setDataSourceWorkloadName(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString());
                        dataSourceWorkload.setDataSourceWorkloadType(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD_TYPE).getAsString());

                        // Add the Workload object to the list for the namespace key
                        namespaceWorkloadMap.computeIfAbsent(namespace, key -> new ArrayList<>()).add(dataSourceWorkload);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON: " + e.getMessage());
        }
        return namespaceWorkloadMap;
    }

    /**
     * Parses container metric information from a JsonArray and organizes it into a HashMap
     * with namespaces as keys and lists of DataSourceContainers objects as values.
     *
     * @param resultArray The JsonArray containing the container information.
     * @return A HashMap<String, List<DataSourceContainers>> representing namespaces
     *         and their associated container details.
     *
     * Example:
     * input dataObject structure:
     * {
     *   "result": [
     *     {
     *       "metric": {
     *         "namespace": "exampleNamespace",
     *         "container": "exampleContainer",
     *         "image_name": "exampleImageName"
     *       }
     *     },
     *     // ... additional result objects ...
     *   ]
     * }
     *
     * The function would parse the JsonObject and return a HashMap like:
     * {
     *   "exampleNamespace": [
     *     {
     *       "container_name": "exampleContainer",
     *       "container_image_name": "exampleImageName",
     *     },
     *     // ... additional DataSourceContainer objects ...
     *   ],
     *   // ... additional namespaces ...
     * }
     */
    public HashMap<String, List<DataSourceContainers>> getContainerInfo(JsonArray resultArray) {
        HashMap<String, List<DataSourceContainers>> workloadContainerMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "workload"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the workload name value
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD)) {
                        String workload = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();

                        // Create Container object and populate
                        DataSourceContainers dataSourceContainers = new DataSourceContainers();
                        dataSourceContainers.setDataSourceContainerName(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME).getAsString());
                        dataSourceContainers.setDataSourceContainerImageName(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_IMAGE_NAME).getAsString());

                        // Add the Container objects to the list for the workload key
                        workloadContainerMap.computeIfAbsent(workload, key -> new ArrayList<>()).add(dataSourceContainers);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON: " + e.getMessage());
        }
        return workloadContainerMap;
    }

    /**
     * Creates and returns a DataSourceDetailsInfo object based on the provided parameters.
     * This function populates the DataSourceDetailsInfo object with information about active namespaces,
     * workload and container information.
     *
     * @param cluster_group_name     Name of the cluster group representing data source provider.
     * @param activeNamespaces       List of active namespace names.
     * @param namespaceWorkloadMap   Mapping of namespaces to lists of DataSourceWorkload objects.
     * @param workloadContainerMap   Mapping of workload names to lists of DataSourceContainers objects.
     * @return                       A DataSourceDetailsInfo object with populated information.
     */
    public DataSourceDetailsInfo createDataSourceDetailsInfoObject(String cluster_group_name, List<String> activeNamespaces,
                                                                   HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap,
                                                                   HashMap<String, List<DataSourceContainers>> workloadContainerMap) {
        // add Datasource constants
        DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo();
        dataSourceDetailsInfo.setVersion(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version);

        DataSourceClusterGroup dataSourceClusterGroup = new DataSourceClusterGroup();
        dataSourceClusterGroup.setDataSourceClusterGroupName(cluster_group_name);

        DataSourceCluster dataSourceCluster = new DataSourceCluster();
        dataSourceCluster.setDataSourceClusterName(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME);

        List<DataSourceNamespace> dataSourceNamespaceList = new ArrayList<>();

        for (String namespaceName : activeNamespaces) {
            DataSourceNamespace namespace = new DataSourceNamespace();
            namespace.setDataSourceNamespaceName(namespaceName);

            List<DataSourceWorkload> dataSourceWorkloadList = namespaceWorkloadMap.getOrDefault(namespaceName, Collections.emptyList());

            for (DataSourceWorkload workload : dataSourceWorkloadList) {
                List<DataSourceContainers> dataSourceContainersList = workloadContainerMap.getOrDefault(workload.getDataSourceWorkloadName(), Collections.emptyList());
                workload.setDataSourceContainers(dataSourceContainersList);
            }

            namespace.setDataSourceWorkloads(dataSourceWorkloadList);
            dataSourceNamespaceList.add(namespace);
        }

        dataSourceCluster.setDataSourceNamespaces(dataSourceNamespaceList);
        dataSourceClusterGroup.setDataSourceCluster(dataSourceCluster);
        dataSourceDetailsInfo.setDataSourceClusterGroup(dataSourceClusterGroup);

        return dataSourceDetailsInfo;
    }
}


