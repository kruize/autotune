package com.autotune.common.data.dataSourceDetails;

import com.autotune.common.exceptions.DataSourceDetailsMissingRequiredField;
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
    public List<DataSourceNamespace> getActiveNamespaces(JsonArray resultArray) {
        List<DataSourceNamespace> namespaces = new ArrayList<>();

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

                            DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(namespace);
                            namespaces.add(dataSourceNamespace);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing namespace JSON array: " + e.getMessage());
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
                        DataSourceWorkload dataSourceWorkload = new DataSourceWorkload(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD).getAsString(), metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.WORKLOAD_TYPE).getAsString());
                        // Add the Workload object to the list for the namespace key
                        namespaceWorkloadMap.computeIfAbsent(namespace, key -> new ArrayList<>()).add(dataSourceWorkload);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing workload JSON array: " + e.getMessage());
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
                        DataSourceContainers dataSourceContainers = new DataSourceContainers(metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_NAME).getAsString(), metricObject.get(KruizeConstants.DataSourceConstants.DataSourceQueryMetricKeys.CONTAINER_IMAGE_NAME).getAsString());
                        // Add the Container objects to the list for the workload key
                        workloadContainerMap.computeIfAbsent(workload, key -> new ArrayList<>()).add(dataSourceContainers);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing container JSON array: " + e.getMessage());
        }
        return workloadContainerMap;
    }

    /**
     * validates the input parameters before creating dataSourceDetailsInfo objects
     * @param clusterGroupName String containing name of the datasource
     * @param dataSourceNamespaces   List of active namespace names.
     * @param namespaceWorkloadMap   Mapping of namespaces to lists of DataSourceWorkload objects.
     * @param workloadContainerMap   Mapping of workload names to lists of DataSourceContainers objects.
     * @throws DataSourceDetailsMissingRequiredField If any input parameter fails the validation check.
     */
    public void validateInputParameters(String clusterGroupName, List<DataSourceNamespace> dataSourceNamespaces,
                                        HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap,
                                        HashMap<String, List<DataSourceContainers>> workloadContainerMap) {

        try {
            if (clusterGroupName == null || clusterGroupName.isEmpty()) {
                throw new DataSourceDetailsMissingRequiredField(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_CLUSTER_GROUP_NAME);
            }
            if (dataSourceNamespaces == null || dataSourceNamespaces.isEmpty()) {
                throw new DataSourceDetailsMissingRequiredField(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_NAMESPACE_DATA);
            }
            if (namespaceWorkloadMap == null) {
                throw new DataSourceDetailsMissingRequiredField(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_WORKLOAD_DATA);
            }
            if (workloadContainerMap == null) {
                throw new DataSourceDetailsMissingRequiredField(KruizeConstants.DataSourceConstants.DataSourceDetailsErrorMsgs.MISSING_DATASOURCE_DETAILS_CONTAINER_DATA);
            }

        } catch (DataSourceDetailsMissingRequiredField e) {
            LOGGER.error(e.getMessage());
        }
    }
    /**
     * Creates and returns a DataSourceDetailsInfo object based on the provided parameters.
     * This function populates the DataSourceDetailsInfo object with information about active namespaces,
     * workload and container information.
     *
     * @param clusterGroupName       Name of the cluster group representing data source provider.
     * @param dataSourceNamespaces   List of active namespace names.
     * @param namespaceWorkloadMap   Mapping of namespaces to lists of DataSourceWorkload objects.
     * @param workloadContainerMap   Mapping of workload names to lists of DataSourceContainers objects.
     * @return                       A DataSourceDetailsInfo object with populated information.
     */
    public DataSourceDetailsInfo createDataSourceDetailsInfoObject(String clusterGroupName, List<DataSourceNamespace> dataSourceNamespaces,
                                                                   HashMap<String, List<DataSourceWorkload>> namespaceWorkloadMap,
                                                                   HashMap<String, List<DataSourceContainers>> workloadContainerMap) throws DataSourceDetailsInfoCreationException {
        try {
            validateInputParameters(clusterGroupName, dataSourceNamespaces, namespaceWorkloadMap, workloadContainerMap);

            DataSourceDetailsInfo dataSourceDetailsInfo = new DataSourceDetailsInfo(
                    KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.version,
                    new DataSourceClusterGroup(
                            clusterGroupName,
                            new DataSourceCluster(
                                    KruizeConstants.DataSourceConstants.DataSourceDetailsInfoConstants.CLUSTER_NAME
                            )
                    )
            );

            // Metadata population
            for (DataSourceNamespace dataSourceNamespace : dataSourceNamespaces) {
                String namespaceName = dataSourceNamespace.getDataSourceNamespaceName();
                List<DataSourceWorkload> dataSourceWorkloadList = namespaceWorkloadMap.getOrDefault(namespaceName, Collections.emptyList());

                for (DataSourceWorkload workload : dataSourceWorkloadList) {
                    List<DataSourceContainers> dataSourceContainersList = workloadContainerMap.getOrDefault(workload.getDataSourceWorkloadName(), Collections.emptyList());
                    workload.setDataSourceContainers(dataSourceContainersList);
                }

                dataSourceNamespace.setDataSourceWorkloads(dataSourceWorkloadList);
            }

            dataSourceDetailsInfo.getDataSourceClusterGroup().getDataSourceCluster().setDataSourceNamespaces(dataSourceNamespaces);

            return dataSourceDetailsInfo;
        } catch (Exception e) {
            throw new DataSourceDetailsInfoCreationException("Error creating DataSourceDetailsInfo: " + e.getMessage());
        }
    }
}


