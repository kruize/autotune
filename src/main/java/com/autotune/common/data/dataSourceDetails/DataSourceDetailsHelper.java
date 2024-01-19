package com.autotune.common.data.dataSourceDetails;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class DataSourceDetailsHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceDetailsHelper.class);

    /**
     * Parses namespace information from a JsonObject and organizes
     * into a List of namespaces
     *
     * @param dataObject The JsonObject containing the namespace information.
     * @return A List<String> representing namespaces
     *
     * Example:
     * input dataObject structure:
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
    public List<String> parseActiveNamespaces(JsonObject dataObject) {
        List<String> namespaces = new ArrayList<>();

        try {
            // Check if the response data contains "result" as an array
            if (dataObject.has("result") && dataObject.get("result").isJsonArray()) {
                JsonArray resultArray = dataObject.getAsJsonArray("result");

                // Iterate through the "result" array to extract namespaces
                for (JsonElement result : resultArray) {
                    if (result.isJsonObject()) {
                        JsonObject resultObject = result.getAsJsonObject();

                        // Check if the result object contains the "metric" field with "namespace"
                        if (resultObject.has("metric") && resultObject.get("metric").isJsonObject()) {
                            JsonObject metricObject = resultObject.getAsJsonObject("metric");

                            // Extract the namespace value and add it to the list
                            if (metricObject.has("namespace")) {
                                String namespace = metricObject.get("namespace").getAsString();
                                namespaces.add(namespace);
                            }
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
     * Parses workload information from a JsonObject and organizes it into a HashMap
     * with namespaces as keys and lists of DataSourceWorkload objects as values.
     *
     * @param dataObject The JsonObject containing the workload information.
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
    public HashMap<String, List<DataSourceWorkload>> parseWorkloadInfo(JsonObject dataObject) {
        HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap = new HashMap<>();

        try {
            // Check if the response data contains "result" as an array
            if (dataObject.has("result") && dataObject.get("result").isJsonArray()) {
                JsonArray resultArray = dataObject.getAsJsonArray("result");

                // Iterate through the "result" array to extract namespaces
                for (JsonElement result : resultArray) {
                    JsonObject resultObject = result.getAsJsonObject();

                    // Check if the result object contains the "metric" field with "namespace"
                    if (resultObject.has("metric") && resultObject.get("metric").isJsonObject()) {
                        JsonObject metricObject = resultObject.getAsJsonObject("metric");

                        // Extract the namespace value
                        if (metricObject.has("namespace")) {
                            String namespace = metricObject.get("namespace").getAsString();

                            // Create Workload object and populate it
                            DataSourceWorkload dataSourceWorkload = new DataSourceWorkload();
                            dataSourceWorkload.setDataSourceWorkloadName(metricObject.get("workload").getAsString());
                            dataSourceWorkload.setDataSourceWorkloadType(metricObject.get("workload_type").getAsString());

                            // Add the Workload object to the list for the namespace
                            namespaceWorkloadMap.computeIfAbsent(namespace, key -> new ArrayList<>()).add(dataSourceWorkload);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON: " + e.getMessage());
        }
        return namespaceWorkloadMap;
    }

    /**
     * Parses container metric information from a JsonObject and organizes it into a HashMap
     * with namespaces as keys and lists of DataSourceContainers objects as values.
     *
     * @param dataObject The JsonObject containing the container information.
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
    public HashMap<String, List<DataSourceContainers>> parseContainerInfo(JsonObject dataObject) {
        HashMap<String, List<DataSourceContainers>> workloadContainerMap = new HashMap<>();

        try {
            // Check if the response data contains "result" as an array
            if (dataObject.has("result") && dataObject.get("result").isJsonArray()) {
                JsonArray resultArray = dataObject.getAsJsonArray("result");

                // Iterate through the "result" array to extract namespaces
                for (JsonElement result : resultArray) {
                    JsonObject resultObject = result.getAsJsonObject();

                    // Check if the result object contains the "metric" field with "namespace"
                    if (resultObject.has("metric") && resultObject.get("metric").isJsonObject()) {
                        JsonObject metricObject = resultObject.getAsJsonObject("metric");

                        // Extract the namespace value
                        if (metricObject.has("workload")) {
                            String workload = metricObject.get("workload").getAsString();

                            // Create Containers object and populate it
                            DataSourceContainers dataSourceContainers = new DataSourceContainers();
                            dataSourceContainers.setDataSourceContainerName(metricObject.get("container").getAsString());
                            dataSourceContainers.setDataSourceContainerImageName(metricObject.get("image").getAsString());

                            // Add the Containers object to the list for the namespace
                            workloadContainerMap.computeIfAbsent(workload, key -> new ArrayList<>()).add(dataSourceContainers);
                        }
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
     * @param activeNamespaces       List of active namespace names.
     * @param namespaceWorkloadMap   Mapping of namespace names to lists of DataSourceWorkload objects.
     * @param workloadContainerMap   Mapping of workload names to lists of DataSourceContainers objects.
     * @return                       A DataSourceDetailsInfo object with populated information.
     */
    public DataSourceDetailsInfo createDataSourceDetailsInfoObject(List<String> activeNamespaces,
                                                                   HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap,
                                                                   HashMap<String, List<DataSourceContainers>> workloadContainerMap) {

        DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo();
        dataSourceDetailsInfo.setVersion("1.0");

        DataSourceClusterGroup dataSourceClusterGroup = new DataSourceClusterGroup();
        dataSourceClusterGroup.setDataSourceClusterGroupName("prometheus");

        DataSourceCluster dataSourceCluster = new DataSourceCluster();
        dataSourceCluster.setDataSourceClusterName("k8s-cluster");

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


