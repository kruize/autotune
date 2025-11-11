package com.autotune.analyzer.services;

import com.autotune.analyzer.Layer.Layer;
import com.autotune.analyzer.Layer.LayerDetector;
import com.autotune.analyzer.Layer.LayerPresence;
import com.autotune.common.datasource.DataSourceCollection;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.k8sObjects.K8sObject;
import com.autotune.database.service.ExperimentDBService;
import com.autotune.utils.GenericRestApiClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to validate layer PromQL queries and return reachable layers
 */
public class LayerDetectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerDetectionService.class);
    private final ExperimentDBService experimentDBService;

    public LayerDetectionService() {
        this.experimentDBService = new ExperimentDBService();
    }

    /**
     * Fetches all layers from DB and validates PromQL queries
     * Returns only layers with reachable datasources
     *
     * @param dataSourceInfoMap Map of datasource names to DataSourceInfo objects
     * @return List of validated Layer objects that are reachable
     */
    public List<Layer> fetchAndDetectLayers(Map<String, DataSourceInfo> dataSourceInfoMap, String containerName, K8sObject k8sObject) {
        List<Layer> detectedLayers = new ArrayList<>();
        Map<String, Layer> layerMap = new HashMap<>();

        try {
            // Use existing loadLayers() to fetch all layers from DB
            experimentDBService.loadLayers(layerMap, null);

            LOGGER.info("Fetched {} layers from database", layerMap.size());

            // detect each layer and add to result if successful
            for (Map.Entry<String, Layer> entry : layerMap.entrySet()) {
                Layer layer = entry.getValue();
                String layerName = layer.getMetadata().getName();
                if (detectLayer(layer, dataSourceInfoMap, containerName, k8sObject)) {
                    detectedLayers.add(layer);
                    LOGGER.info("Layer {} detected successfully", layerName);
                } else {
                    LOGGER.warn("Layer {} detection failed", layerName);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error fetching and validating layers: {}", e.getMessage(), e);
        }

        return detectedLayers;
    }


    /**
     * Detects a layer by checking PromQL query reachability
     *
     * @param layer Layer object to validate
     * @param dataSourceInfoMap Map of datasource names to DataSourceInfo objects
     * @return true if layer is valid and reachable, false otherwise
     */
    private boolean detectLayer(Layer layer, Map<String, DataSourceInfo> dataSourceInfoMap, String containerName, K8sObject k8sObject) {
        try {
            LayerPresence layerPresence = layer.getLayerPresence();

            if (layerPresence == null) {
                LOGGER.info("Layer {} has no layer_presence defined",
                        layer.getMetadata().getName());
                return false;
            }

            List<LayerDetector> detectors = layerPresence.getDetectors();

            // If detectors are null or empty, consider the layer valid
            if (null == detectors || detectors.isEmpty()) {
                LOGGER.info("Layer {} has no detectors defined, accepting as valid",
                        layer.getMetadata().getName());
                return true;
            }

            // Validate only the first detector - for hackathon scope
            LayerDetector detector = detectors.get(0);
            String detectorType = detector.getType();

            if ("query".equalsIgnoreCase(detectorType)) {
                String datasourceName = detector.getDatasource();
                String query = detector.getQuery();

                if (datasourceName == null || query == null) {
                    LOGGER.error("Layer {} missing datasource or query information",
                            layer.getMetadata().getName());
                    return false;
                }

                // Appending only container and namespace filters for the hackathon scope
                String namespace = k8sObject.getNamespace();

                if (namespace != null && !namespace.isEmpty()) {
                    query = appendFilter(query, "namespace", namespace);
                }

                // Append container filter to query if containerName is provided
                if (containerName != null && !containerName.isEmpty()) {
                    query = appendFilter(query, "container", containerName);
                }

                return validatePromQLQuery(query, datasourceName, dataSourceInfoMap);
            } else {
                // For non-query detectors (e.g., label-based), consider valid by default
                LOGGER.debug("Layer {} uses {} detector type, skipping query validation",
                        layer.getMetadata().getName(), detectorType);
                return true;
            }

        } catch (Exception e) {
            LOGGER.error("Error validating layer {}: {}",
                    layer.getMetadata().getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Appends a label filter to PromQL query
     *
     * @param query Original PromQL query
     * @param labelName Label name (e.g., "container", "namespace")
     * @param labelValue Label value
     * @return Modified query with filter appended
     */
    private String appendFilter(String query, String labelName, String labelValue) {
        String filter = labelName + "=\"" + labelValue + "\"";

        // If query has existing filters, append to FIRST occurrence only
        if (query.contains("{") && query.contains("}")) {
            int openBrace = query.indexOf("{");
            int closeBrace = query.indexOf("}", openBrace);

            // Check if there's content between the first braces
            String existingFilters = query.substring(openBrace + 1, closeBrace).trim();

            String beforeBrace = query.substring(0, closeBrace);
            String afterBrace = query.substring(closeBrace);

            if (existingFilters.isEmpty()) {
                // Empty braces, add filter without comma
                return beforeBrace + filter + afterBrace;
            } else {
                // Has existing filters, append with comma
                return beforeBrace + "," + filter + afterBrace;
            }
        }

        // If no filters, add filter after metric name
        int insertPos = query.indexOf(" ");
        if (insertPos == -1) {
            insertPos = query.length();
        }

        return query.substring(0, insertPos) + "{" + filter + "}" +
               query.substring(insertPos);
    }

    /**
     * Validates PromQL query reachability using GenericRestApiClient
     *
     * @param query PromQL query to validate
     * @param datasourceName Datasource name
     * @param dataSourceInfoMap Map of datasource info
     * @return true if query is reachable, false otherwise
     */
    private boolean validatePromQLQuery(String query, String datasourceName,
                                        Map<String, DataSourceInfo> dataSourceInfoMap) {
        try {
            DataSourceInfo dataSourceInfo = dataSourceInfoMap.get(datasourceName);

            if (dataSourceInfo == null) {
                LOGGER.error("DataSource {} not found", datasourceName);
                return false;
            }

            // Create GenericRestApiClient with authentication
            GenericRestApiClient restClient = new GenericRestApiClient(dataSourceInfo);

            // Set base URL for Prometheus query API
            String baseUrl = String.valueOf(dataSourceInfo.getUrl());
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            baseUrl += "api/v1/query?query=";
            restClient.setBaseURL(baseUrl);

            LOGGER.info("Validating promQL query: {}", query);

            // Execute the query
            JSONObject response = restClient.fetchMetricsJson("GET", query);
            LOGGER.info("response: {}", response);
            boolean isValid = response != null &&
                    response.has("status") &&
                    "success".equalsIgnoreCase(response.getString("status")) &&
                    response.has("data") &&
                    response.getJSONObject("data").has("result") &&
                    !response.getJSONObject("data").getJSONArray("result").isEmpty();
            if (isValid) {
                LOGGER.debug("PromQL query validation successful for datasource {}",
                        datasourceName);
            } else {
                LOGGER.info("Invalid query response from datasource {}", datasourceName);
            }
            return isValid;
        } catch (Exception e) {
            LOGGER.error("Error validating PromQL query for datasource {}: {}",
                    datasourceName, e.getMessage());
            return false;
        }
    }
    public HashMap<String, Layer> detectAllLayers(String containerName, K8sObject k8sObject) {
        // Load datasources from DB
        Map<String, DataSourceInfo> dataSourceMap = DataSourceCollection.getInstance().getDataSourcesCollection();
        // Validate and get only reachable layers
        List<Layer> validLayers = fetchAndDetectLayers(dataSourceMap, containerName, k8sObject);
        HashMap<String, Layer> detectedLayers = new HashMap<>();

        for (Layer layer : validLayers) {
            String name = layer.getMetadata().getName();
            detectedLayers.put(name, layer);
        }

        return detectedLayers;
    }
}