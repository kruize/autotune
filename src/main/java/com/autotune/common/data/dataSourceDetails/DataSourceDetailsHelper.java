package com.autotune.common.data.dataSourceDetails;

import com.autotune.utils.KruizeConstants;
import com.autotune.common.exceptions.DataSourceDetailsInfoCreationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

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
     */
    public HashMap<String, DataSourceNamespace> getActiveNamespaces(JsonArray resultArray) {
        HashMap<String, DataSourceNamespace> dataSourceNamespaceHashMap = new HashMap<>();

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

                            DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespace);
                            dataSourceNamespaceHashMap.put(namespace, dataSourceNamespace);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing namespace JSON array: " + e.getMessage());
        }
        return dataSourceNamespaceHashMap;
    }

    /**
     * Parses workload information from a JsonArray and organizes it into a HashMap
     * with namespaces as keys and DataSourceWorkload objects as values.
     *
     * @param resultArray The JsonArray containing the workload information.
     * @return A HashMap<String, DataSourceWorkload> representing namespaces
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
     *       "containers": null
     *     },
     *     // ... additional DataSourceWorkload objects ...
     *   ],
     *   // ... additional namespaces ...
     * }
     */
    public HashMap<String, DataSourceWorkload> getWorkloadInfo(JsonArray resultArray) {
        HashMap<String, DataSourceWorkload> dataSourceWorkloadHashMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "namespace"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the workload name value
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD)) {
                        String workloadName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString();

                        if (!dataSourceWorkloadHashMap.containsKey(workloadName)) {
                            String workloadType = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD_TYPE).getAsString();
                            DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(workloadName, workloadType);
                            dataSourceWorkloadHashMap.put(workloadName, dataSourceWorkload);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing workload JSON array: " + e.getMessage());
        }
        return dataSourceWorkloadHashMap;
    }

    /**
     * Parses container metric information from a JsonArray and organizes it into a HashMap
     * with workload as keys and DataSourceContainers objects as values.
     *
     * @param resultArray The JsonArray containing the container information.
     * @return A HashMap<String, DataSourceContainer> representing workloads
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
    public HashMap<String, DataSourceContainer> getContainerInfo(JsonArray resultArray) {
        HashMap<String, DataSourceContainer> dataSourceContainersHashMap = new HashMap<>();

        try {
            // Iterate through the "result" array to extract namespaces
            for (JsonElement result : resultArray) {
                JsonObject resultObject = result.getAsJsonObject();

                // Check if the result object contains the "metric" field with "workload"
                if (resultObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC) && resultObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC).isJsonObject()) {
                    JsonObject metricObject = resultObject.getAsJsonObject(KruizeConstants.DataSourceConstants.DataSourceQueryJSONKeys.METRIC);

                    // Extract the container name value
                    if (metricObject.has(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME)) {
                        String containerName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME).getAsString();
                        if (!dataSourceContainersHashMap.containsKey(containerName)) {
                            String containerImageName = metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_IMAGE_NAME).getAsString();
                            DataSourceContainer dataSourceContainer = new DataSourceContainer(containerName, containerImageName);
                            dataSourceContainersHashMap.put(containerName, dataSourceContainer);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing container JSON array: " + e.getMessage());
        }
        return dataSourceContainersHashMap;
    }

    /**
     * Creates and returns a DataSourceDetailsInfo object based on the provided parameters.
     * This function populates the DataSourceDetailsInfo object with information about active namespaces,
     * workload and container information.
     *
     * @param clusterGroupName       Name of the cluster group representing data source provider.
     * @param dataSourceNamespaces   List of active namespace names.
     * @return                       A DataSourceDetailsInfo object with populated information.
     */
    public DataSourceDetailsInfo createDataSourceDetailsInfoObject(String clusterGroupName, HashMap<String,DataSourceNamespace> dataSourceNamespaces) throws DataSourceDetailsInfoCreationException {
        try {

            DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version);

            DataSourceClusterGroup dataSourceClusterGroup = new DataSourceClusterGroup(clusterGroupName);

            DataSourceCluster dataSourceCluster = new DataSourceCluster(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME, dataSourceNamespaces);

            // Set cluster in cluster group
            HashMap<String, DataSourceCluster> clusters = new HashMap<>();
            clusters.put(KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME, dataSourceCluster);
            dataSourceClusterGroup.setDataSourceCluster(clusters);

            // Set cluster group in DataSourceDetailsInfo
            HashMap<String, DataSourceClusterGroup> clusterGroups = new HashMap<>();
            clusterGroups.put(clusterGroupName, dataSourceClusterGroup);
            dataSourceDetailsInfo.setDataSourceClusterGroup(clusterGroups);


            return dataSourceDetailsInfo;
        } catch (Exception e) {
            throw new DataSourceDetailsInfoCreationException("Error creating DataSourceDetailsInfo: " + e.getMessage());
        }
    }
}


