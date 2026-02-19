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
package com.autotune.analyzer.recommendations.engine;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.kruizeLayer.Tunable;
import com.autotune.analyzer.kruizeLayer.impl.TunableDependencyResolver;
import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.model.GenericRecommendationModel;
import com.autotune.analyzer.recommendations.model.layers.TunableRecommendationGenerator;
import com.autotune.analyzer.recommendations.model.layers.HotspotRecommendationGenerator;
import com.autotune.analyzer.recommendations.model.layers.SemeruRecommendationGenerator;
import com.autotune.analyzer.recommendations.model.layers.QuarkusRecommendationGenerator;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.kruizeLayer.utils.LayerUtils;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;

/**
 * Orchestrates layer-specific recommendation generation.
 * Loops over orderedTunables which were generated based on Experiment and dependencyresolver
 * and delegates to layer-specific generators and returns all recommendations (container + runtime)
 */
public class LayerRecommendationOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerRecommendationOrchestrator.class);

    private static final String ENV_JDK_JAVA_OPTIONS = "JDK_JAVA_OPTIONS";
    private static final String ENV_JAVA_OPTIONS = "JAVA_OPTIONS";
    
    private static final String LAYER_HOTSPOT = "hotspot";
    private static final String LAYER_SEMERU = "semeru";

    /**
     * Checks if a layer is a JVM-based runtime that requires flag combination.
     * JVM-based runtimes: Hotspot, Semeru
     * Non-JVM runtimes would handle flags differently.
     */
    private static boolean isJvmBasedRuntime(String layerName) {
        String layerNameLower = layerName.toLowerCase();
        return LAYER_HOTSPOT.equals(layerNameLower) ||
               LAYER_SEMERU.equals(layerNameLower);
    }

    private static final Map<String, TunableRecommendationGenerator> GENERATOR_REGISTRY;
    
    static {
        // Initialize registry once at class load time for performance
        Map<String, TunableRecommendationGenerator> registry = new HashMap<>();
        registerDefaultGenerators(registry);
        GENERATOR_REGISTRY = Map.copyOf(registry);
    }
    
    /**
     * Registers default generators for known layers (called once during static initialization)
     */
    private static void registerDefaultGenerators(Map<String, TunableRecommendationGenerator> registry) {
        TunableRecommendationGenerator hotspotGen = new HotspotRecommendationGenerator();
        TunableRecommendationGenerator semeruGen = new SemeruRecommendationGenerator();
        TunableRecommendationGenerator quarkusGen = new QuarkusRecommendationGenerator();
        
        registry.put(hotspotGen.getLayerName(), hotspotGen);
        registry.put(semeruGen.getLayerName(), semeruGen);
        registry.put(quarkusGen.getLayerName(), quarkusGen);
        
        LOGGER.info("Registered {} recommendation generators", registry.size());
    }
    
    /**
     * Generates all recommendations for a specific model.
     * @param model The recommendation model (cost or performance)
     * @param kruizeObject Experiment object with datasource and layer metadata
     * @param filteredResultsMap Metrics data for the term period
     * @param notifications List to collect any notifications/warnings
     * @return Map with keys:
     *         - "container": Map<Object, RecommendationConfigItem> (CPU/Memory/GPU recommendations)
     *         - "runtime": List<RecommendationConfigEnv> (JVM and Quarkus flags)
     */
    public static Map<String, Object> generateAllRecommendations(
            GenericRecommendationModel model,
            KruizeObject kruizeObject,
            Map<Timestamp, IntervalResults> filteredResultsMap,
            List<RecommendationConstants.RecommendationNotification> notifications) {

        // 1. Detect layers from experiment metadata
        LOGGER.debug("Detecting layers from experiment metadata");
        String containerName = kruizeObject.getKubernetes_objects().get(0).getContainerDataMap().keySet().iterator().next();
        String namespace = kruizeObject.getKubernetes_objects().get(0).getNamespace();
        String datasourceName = kruizeObject.getDataSource();
        
        Map<String, KruizeLayer> detectedLayers = null;
        try {
            detectedLayers = LayerUtils.detectLayers(containerName, namespace, datasourceName);
        } catch (Exception e) {
            LOGGER.error("Failed to detect layers: {}", e.getMessage());
            return Collections.emptyMap();
        }
        
        if (detectedLayers == null || detectedLayers.isEmpty()) {
            LOGGER.debug("No layers detected, returning empty recommendations");
            return Map.of("container", new HashMap<>(), "runtime", new ArrayList<>());
        }
        
        LOGGER.info("Generating recommendations for {}:{} with {} for {} detected layer(s): {}",
                model,containerName, datasourceName, detectedLayers.size(), detectedLayers.keySet());
        
        // 2. Resolve tunable dependencies to determine calculation order
        List<TunableSpec> orderedTunableSpecs;
        try {
            orderedTunableSpecs = TunableDependencyResolver.resolve(new ArrayList<>(detectedLayers.values()));
        } catch (Exception e) {
            LOGGER.error("Failed to resolve tunable dependencies: {}", e.getMessage(), e);
            return Map.of("container", new HashMap<>(), "runtime", new ArrayList<>());
        }
        
        LOGGER.info("Resolved {} tunable specs in dependency order: {}",
                orderedTunableSpecs.size(),
                orderedTunableSpecs.stream().map(TunableSpec::toString).collect(java.util.stream.Collectors.toList()));
        
        // 3. Initialize dependency values map (generators will extract JVM metadata as needed)
        Map<TunableSpec, Object> dependencyValues = new HashMap<>();
        
        // 4. Initialize container and runtime recommendations maps
        Map<Object, RecommendationConfigItem> allContainerRecs = new HashMap<>();
        List<RecommendationConfigEnv> runtimeRecs = new ArrayList<>();
        
        // 5. Initialize runtime flags accumulator for JVM-based runtimes
        Map<String, StringBuilder> runtimeFlagsByLayer = new HashMap<>();
        for (String layerName : detectedLayers.keySet()) {
            if (isJvmBasedRuntime(layerName)) {
                runtimeFlagsByLayer.put(layerName.toLowerCase(), new StringBuilder());
            }
        }
        
        // 6. Loop over orderedTunableSpecs and generate recommendations
        for (TunableSpec tunableSpec : orderedTunableSpecs) {
            String tunableName = tunableSpec.tunableName();
            String layerName = tunableSpec.layerName();
            
            // Handle container tunable: MEMORY
            boolean isMemoryTunable = AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_REQUEST.equals(tunableName) ||
                    AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT.equals(tunableName);

            if (isMemoryTunable) {
                String requestKey = AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_REQUEST;
                String limitKey = AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT;
                if (!allContainerRecs.containsKey(requestKey) && !allContainerRecs.containsKey(limitKey)) {
                    LOGGER.debug("Generating memory recommendation");
                    ArrayList<RecommendationNotification> memNotifications = new ArrayList<>();
                    for (int i = 0; i < notifications.size(); i++) {
                        memNotifications.add(new RecommendationNotification(notifications.get(i)));
                    }
                    RecommendationConfigItem memRec = model.getMemoryRequestRecommendation(filteredResultsMap, memNotifications);
                    if (memRec != null) {
                        allContainerRecs.put(requestKey, memRec);
                        allContainerRecs.put(limitKey, memRec);

                        dependencyValues.put(new TunableSpec(layerName, requestKey), memRec.getAmount());
                        dependencyValues.put(new TunableSpec(layerName, limitKey), memRec.getAmount());
                        LOGGER.info("Memory recommendation: {} {}", memRec.getAmount(), memRec.getFormat());
                    }
                } else {
                    LOGGER.debug("Skipping {} - Memory recommendation already processed.", tunableName);
                }
                continue;
            }

            // Handle container tunable: CPU
            boolean isCpuTunable = AnalyzerConstants.LayerConstants.TunablesConstants.CPU_REQUEST.equals(tunableName) ||
                    AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT.equals(tunableName);

            if (isCpuTunable) {
                String requestKey = AnalyzerConstants.LayerConstants.TunablesConstants.CPU_REQUEST;
                String limitKey = AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT;
                if (!allContainerRecs.containsKey(requestKey) && !allContainerRecs.containsKey(limitKey)) {
                    LOGGER.debug("Generating CPU recommendation");
                    ArrayList<RecommendationNotification> cpuNotifications = new ArrayList<>();
                    for (int i = 0; i < notifications.size(); i++) {
                        cpuNotifications.add(new RecommendationNotification(notifications.get(i)));
                    }
                    RecommendationConfigItem cpuRec = model.getCPURequestRecommendation(filteredResultsMap, cpuNotifications);
                    if (cpuRec != null) {
                        allContainerRecs.put(requestKey, cpuRec);
                        allContainerRecs.put(limitKey, cpuRec);

                        dependencyValues.put(new TunableSpec(layerName, requestKey), cpuRec.getAmount());
                        dependencyValues.put(new TunableSpec(layerName, limitKey), cpuRec.getAmount());
                        LOGGER.info("CPU recommendation: {} {}", cpuRec.getAmount(), cpuRec.getFormat());
                    }
                } else {
                    LOGGER.debug("Skipping {} - Cpu recommendation already processed.", tunableName);
                }
                continue;
            }
            
            // Handle runtime tunables
            if (layerName == null) {
                LOGGER.warn("Could not determine layer for tunable: {}", tunableName);
                continue;
            }

            try {
                TunableRecommendationGenerator generator = GENERATOR_REGISTRY.get(layerName);
                if (generator == null) {
                    LOGGER.warn("No generator for layer: {}. Skipping tunable: {}", layerName, tunableName);
                    continue;
                }
                
                // Get the actual Tunable object from the layer
                KruizeLayer layer = detectedLayers.get(layerName);
                if (layer == null) {
                    LOGGER.warn("Layer not found: {}. Skipping tunable: {}", layerName, tunableName);
                    continue;
                }
                
                Tunable tunable = layer.getTunables().stream()
                        .filter(t -> t.getName().equals(tunableName))
                        .findFirst()
                        .orElse(null);
                
                if (tunable == null) {
                    LOGGER.warn("Tunable not found in layer {}: {}", layerName, tunableName);
                    continue;
                }
                
                // Generate recommendation (pass filteredResultsMap for JVM metadata extraction)
                RecommendationConfigEnv recommendation = generator.generateRecommendation(tunable, dependencyValues, filteredResultsMap);
                
                if (recommendation != null) {
                    // For JVM-based runtimes, accumulate flags
                    if (isJvmBasedRuntime(layerName)) {
                        StringBuilder flags = runtimeFlagsByLayer.get(layerName.toLowerCase());
                        if (flags != null) {
                            flags.append(recommendation.getValue()).append(" ");
                            LOGGER.debug("Accumulated flag for {}: {}", layerName, recommendation.getValue());
                        }
                    } else {
                        // For non-JVM layers, add directly
                        runtimeRecs.add(recommendation);
                        LOGGER.debug("Added recommendation for {}.{}: {}",
                                layerName, tunableName, recommendation.getValue());
                    }
                    
                    // Store numeric value for dependent tunables (use tunableSpec from loop)
                    Object numericValue = extractNumericValue(tunableName, recommendation.getValue());
                    dependencyValues.put(tunableSpec, numericValue != null ? numericValue : recommendation.getValue());
                }
            } catch (Exception e) {
                LOGGER.error("Error generating recommendation for {}.{}: {}",
                        layerName, tunableName, e.getMessage(), e);
            }
        }
        
        // 8. Combine JVM flags into environment variables
        for (Map.Entry<String, StringBuilder> entry : runtimeFlagsByLayer.entrySet()) {
            StringBuilder flagsBuilder = entry.getValue();
            if (flagsBuilder.length() > 0) {
                // Remove trailing space
                if (flagsBuilder.charAt(flagsBuilder.length() - 1) == ' ') {
                    flagsBuilder.setLength(flagsBuilder.length() - 1);
                }
                String combinedFlags = flagsBuilder.toString();
                
                // Add to both JDK_JAVA_OPTIONS and JAVA_OPTIONS
                runtimeRecs.add(new RecommendationConfigEnv(ENV_JDK_JAVA_OPTIONS, combinedFlags));
                runtimeRecs.add(new RecommendationConfigEnv(ENV_JAVA_OPTIONS, combinedFlags));
                LOGGER.info("Combined {} runtime flags: {}", entry.getKey(), combinedFlags);
                break; // Only one JVM runtime per container
            }
        }
        
        LOGGER.info("Generated {} runtime recommendations", runtimeRecs.size());
        
        // 10. Return all recommendations
        return Map.of("container", allContainerRecs, "runtime", runtimeRecs);
    }
    
    /**
     * Extracts numeric value from tunable recommendation string for dependency tracking.
     * @param tunableName The tunable name
     * @param value The recommendation value string
     * @return Numeric value if extractable, null otherwise
     */
    private static Object extractNumericValue(String tunableName, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // Extract value after '=' if present
            if (value.contains("=")) {
                String[] parts = value.split("=");
                if (parts.length == 2) {
                    try {
                        return Double.parseDouble(parts[1]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            
            // Try parsing the whole value as a number
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to extract numeric value from {}: {}", tunableName, value);
            return null;
        }
    }
}