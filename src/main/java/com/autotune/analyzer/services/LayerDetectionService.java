package com.autotune.analyzer.services;

import com.autotune.analyzer.Layer.Layer;
import com.autotune.analyzer.Layer.LayerDetector;
import com.autotune.analyzer.Layer.LayerPresence;
import com.autotune.common.datasource.DataSourceInfo;
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
    public List<Layer> fetchAndDetectLayers(Map<String, DataSourceInfo> dataSourceInfoMap) {
        List<Layer> detectedLayers = new ArrayList<>();
        Map<String, Layer> layerMap = new HashMap<>();

        try {
            // Use existing loadLayers() to fetch all layers from DB
            experimentDBService.loadLayers(layerMap, null);

            LOGGER.info("Loaded {} layers from database", layerMap.size());

            // detect each layer and add to result if successful
            for (Map.Entry<String, Layer> entry : layerMap.entrySet()) {
                Layer layer = entry.getValue();
                String layerName = layer.getMetadata().getName();

                if (detectLayer(layer, dataSourceInfoMap)) {
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
    private boolean detectLayer(Layer layer, Map<String, DataSourceInfo> dataSourceInfoMap) {
        try {
            LayerPresence layerPresence = layer.getLayerPresence();

            if (layerPresence == null) {
                LOGGER.info("Layer {} has no layer_presence defined",
                        layer.getMetadata().getName());
                return false;
            }

            List<LayerDetector> detectors = layerPresence.getDetectors();

            // If detectors are null or empty, consider the layer valid
            if (detectors == null || detectors.isEmpty()) {
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

            LOGGER.info("baseUrl is {}", baseUrl);
            LOGGER.info("Validating promQL query: {}", query);

            // Execute the query
            JSONObject response = restClient.fetchMetricsJson("GET", query);
            LOGGER.info("response: {}", response);

            // Check response status
            if (response != null && response.has("status")) {
                String status = response.getString("status");

                if ("success".equalsIgnoreCase(status)) {
                    LOGGER.debug("PromQL query validation successful for datasource {}",
                            datasourceName);
                    return true;
                } else {
                    LOGGER.warn("PromQL query returned status: {}", status);
                    return false;
                }
            } else {
                LOGGER.error("Invalid response format from datasource {}", datasourceName);
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Error validating PromQL query for datasource {}: {}",
                    datasourceName, e.getMessage());
            return false;
        }
    }

    public HashMap<String, Layer> detectAllLayers() throws Exception {
        // Load datasources from DB
        List<DataSourceInfo> dataSources = experimentDBService.loadAllDataSources();

        // Create datasource map
        Map<String, DataSourceInfo> dataSourceMap = new HashMap<>();
        for (DataSourceInfo ds : dataSources) {
            dataSourceMap.put(ds.getName(), ds);
        }

        // Validate and get only reachable layers
        List<Layer> validLayers = fetchAndDetectLayers(dataSourceMap);

        LOGGER.info("Found " + validLayers.size() + " valid layers");
        HashMap<String, Layer> detectedLayers = new HashMap<>();

        for (Layer layer : validLayers) {
            String name = layer.getMetadata().getName();
            detectedLayers.put(name, layer);
        }

        return detectedLayers;
    }
}