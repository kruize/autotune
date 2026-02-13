/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.kruizeLayer.utils;

import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.kruizeLayer.presence.LabelBasedPresence;
import com.autotune.analyzer.kruizeLayer.presence.QueryBasedPresence;
import com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.LogMessages;
import com.autotune.database.service.ExperimentDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for layer detection operations
 */
public class LayerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerUtils.class);

    /**
     * Detects which layers are present for a given container in a namespace
     *
     * @param containerName The container name
     * @param namespace The Kubernetes namespace
     * @return Map of detected layers (layer name -> KruizeLayer).
     *         Returns an empty map when no layers are detected.
     * @throws IllegalArgumentException if containerName or namespace is null or empty
     * @throws Exception if database operations fail
     */
    public static Map<String, KruizeLayer> detectLayers(String containerName,
                                                         String namespace) throws Exception {
        // Validate inputs - fail fast with clear error messages
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalArgumentException(LogMessages.CONTAINER_NAME_NULL_OR_EMPTY);
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException(LogMessages.NAMESPACE_NULL_OR_EMPTY);
        }

        LOGGER.info(LogMessages.DETECTING_LAYERS, containerName, namespace);

        // Load all available layers from database
        ExperimentDBService experimentDBService = new ExperimentDBService();
        Map<String, KruizeLayer> allLayersMap = new HashMap<>();

        try {
            experimentDBService.loadAllLayers(allLayersMap);
        } catch (Exception e) {
            LOGGER.error(LogMessages.FAILED_TO_LOAD_LAYERS, e);
            throw new Exception(LogMessages.FAILED_TO_LOAD_LAYERS_EXCEPTION + ": " + e.getMessage(), e);
        }

        if (allLayersMap.isEmpty()) {
            LOGGER.warn(LogMessages.NO_LAYERS_IN_DB);
            return Collections.emptyMap();
        }

        LOGGER.debug(LogMessages.LOADED_LAYERS_FROM_DB, allLayersMap.size());

        // Detect which layers are present
        Map<String, KruizeLayer> detectedLayers = new HashMap<>();

        for (KruizeLayer layer : allLayersMap.values()) {
            try {
                boolean isDetected = false;

                // Use the layer's presence detector
                if (layer.getLayerPresence() != null && layer.getLayerPresence().getDetector() != null) {
                    // Skip label-based detection as it's not yet implemented; warn to avoid hiding misconfigurations
                    if (layer.getLayerPresence().getDetector() instanceof LabelBasedPresence) {
                        LOGGER.warn(LogMessages.LABEL_BASED_PRESENCE_NOT_IMPLEMENTED,
                                    layer.getLayerName());
                        continue; // Skip to next layer
                    }

                    // For query-based detection, pass container name as well
                    if (layer.getLayerPresence().getDetector() instanceof QueryBasedPresence) {
                        QueryBasedPresence queryDetector = (QueryBasedPresence) layer.getLayerPresence().getDetector();
                        isDetected = queryDetector.detectPresence(namespace, containerName);
                    } else {
                        // For other detector types (Always)
                        isDetected = layer.getLayerPresence().getDetector()
                                .detectPresence(namespace, containerName);
                    }

                    if (isDetected) {
                        detectedLayers.put(layer.getLayerName(), layer);
                        LOGGER.info(LogMessages.LAYER_DETECTED,
                                layer.getLayerName(), containerName);
                    } else {
                        LOGGER.debug(LogMessages.LAYER_NOT_DETECTED,
                                layer.getLayerName(), containerName);
                    }
                } else {
                    LOGGER.warn(LogMessages.NO_PRESENCE_DETECTOR,
                            layer.getLayerName());
                }
            } catch (Exception e) {
                LOGGER.error(LogMessages.ERROR_DETECTING_LAYER,
                        layer.getLayerName(), e.getMessage(), e);
                // Continue to next layer instead of failing completely
            }
        }

        if (detectedLayers.isEmpty()) {
            LOGGER.info(LogMessages.NO_LAYERS_DETECTED,
                    containerName, namespace);
        } else {
            LOGGER.info(LogMessages.LAYERS_DETECTED,
                    detectedLayers.size(), containerName, detectedLayers.keySet());
        }

        return detectedLayers;
    }
}
