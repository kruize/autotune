/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use it except in compliance with the License.
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

import com.autotune.analyzer.kruizeLayer.KruizeLayer;
import com.autotune.analyzer.kruizeLayer.impl.TunableDependencyResolver;
import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.LayerRecommendationHandler;
import com.autotune.analyzer.recommendations.LayerRecommendationHandlerRegistry;
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.ContainerData;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.utils.KruizeConstants;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processes runtime layer recommendations (JVM options, GC policy, etc.) for container experiments.
 */
public final class RuntimeRecommendationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeRecommendationProcessor.class);

    private RuntimeRecommendationProcessor() {
    }

    /**
     * Checks for the presence of runtime, quarkus or any other non-container layers.
     *
     * @param detectedLayers map of layer name to KruizeLayer
     * @return true if any layer other than container is detected
     */
    public static boolean isRuntimeLayerPresent(Map<String, KruizeLayer> detectedLayers) {
        if (detectedLayers == null || detectedLayers.isEmpty()) {
            return false;
        }
        String containerLayer = AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER;
        return detectedLayers.keySet().stream()
                .anyMatch(layer -> layer != null && !layer.equalsIgnoreCase(containerLayer));
    }

    /**
     * Handles runtime recommendations for a container: resolves tunable dependencies, invokes
     * layer handlers for JVM/Quarkus tunables, and returns env config items (JDK_JAVA_OPTIONS,
     * JAVA_OPTIONS, CORE_THREADS).
     *
     * @param kruizeObject               to get the datasource
     * @param containerData              container with layer map
     * @param model                      recommendation model
     * @param filteredResultsMap         interval results
     * @param notifications             notifications list to update
     * @param recommendationCpuRequest   container CPU request recommendation
     * @param recommendationMemRequest   container memory request recommendation
     * @param recommendationCpuLimits    container CPU limit recommendation
     * @param recommendationMemLimits    container memory limit recommendation
     * @return list of runtime config env items, or null on error
     */
    public static List<RecommendationConfigEnv> handleRuntimeRecommendations(
            KruizeObject kruizeObject,
            ContainerData containerData,
            RecommendationModel model,
            Map<Timestamp, IntervalResults> filteredResultsMap,
            ArrayList<com.autotune.analyzer.recommendations.RecommendationNotification> notifications,
            RecommendationConfigItem recommendationCpuRequest,
            RecommendationConfigItem recommendationMemRequest,
            RecommendationConfigItem recommendationCpuLimits,
            RecommendationConfigItem recommendationMemLimits) {

        List<RecommendationConfigEnv> runtimeRecommList = new ArrayList<>();
        String datasourceName = kruizeObject.getDataSource();
        if (datasourceName == null) {
            LOGGER.warn("Datasource missing, skipping runtime recommendations");
            return null;
        }
        Map<String, KruizeLayer> layerMap = containerData.getLayerMap();
        LOGGER.debug("layerMap: {}", new Gson().toJson(layerMap));
        if (layerMap == null || layerMap.isEmpty()) {
            return runtimeRecommList;
        }

        // Pre-populate context with CPU/memory recommendations for tunable processing
        Map<TunableSpec, Object> tunableSpecObjectMap = new HashMap<>();
        String containerLayer = AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER;
        tunableSpecObjectMap.put(new TunableSpec(containerLayer, AnalyzerConstants.MetricNameConstants.MEMORY_REQUEST),
                recommendationMemRequest != null ? recommendationMemRequest.getAmount() : null);
        tunableSpecObjectMap.put(new TunableSpec(containerLayer, AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT),
                recommendationMemLimits != null ? recommendationMemLimits.getAmount() : null);
        tunableSpecObjectMap.put(new TunableSpec(containerLayer, AnalyzerConstants.MetricNameConstants.CPU_REQUEST),
                recommendationCpuRequest != null ? recommendationCpuRequest.getAmount() : null);
        tunableSpecObjectMap.put(new TunableSpec(containerLayer, AnalyzerConstants.MetricNameConstants.CPU_LIMIT),
                recommendationCpuLimits != null ? recommendationCpuLimits.getAmount() : null);

        List<KruizeLayer> kruizeLayers = layerMap.values().stream()
                .filter(layer -> layer.getTunables() != null)
                .collect(Collectors.toList());
        List<TunableSpec> orderedTunables = TunableDependencyResolver.resolve(kruizeLayers);
        RecommendationConfigItem recCpuRequest;
        RecommendationConfigItem recCpuLimits;
        RecommendationConfigItem recMemRequest;
        RecommendationConfigItem recMemLimits;
        Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> recommendationAcceleratorRequestMap = null;

        for (TunableSpec spec : orderedTunables) {
            String layerName = spec.layerName();
            String metricName = spec.tunableName();
            Double amount;
            switch (metricName) {
                case AnalyzerConstants.MetricNameConstants.MEMORY_REQUEST:
                    if (tunableSpecObjectMap.containsKey(spec)) {
                        amount = (Double) tunableSpecObjectMap.get(spec);
                    } else {
                        recMemRequest = model.getMemoryRequestRecommendation(filteredResultsMap, notifications);
                        amount = recMemRequest != null ? recMemRequest.getAmount() : null;
                    }
                    tunableSpecObjectMap.put(spec, amount);
                    break;
                case AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT:
                    if (tunableSpecObjectMap.containsKey(spec)) {
                        amount = (Double) tunableSpecObjectMap.get(spec);
                    } else {
                        recMemLimits = model.getMemoryLimitRecommendation(filteredResultsMap, notifications);
                        amount = recMemLimits != null ? recMemLimits.getAmount() : null;
                    }
                    tunableSpecObjectMap.put(spec, amount);
                    break;
                case AnalyzerConstants.MetricNameConstants.CPU_REQUEST:
                    if (tunableSpecObjectMap.containsKey(spec)) {
                        amount = (Double) tunableSpecObjectMap.get(spec);
                    } else {
                        recCpuRequest = model.getCPURequestRecommendation(filteredResultsMap, notifications);
                        amount = recCpuRequest != null ? recCpuRequest.getAmount() : null;
                    }
                    tunableSpecObjectMap.put(spec, amount);
                    break;
                case AnalyzerConstants.MetricNameConstants.CPU_LIMIT:
                    if (tunableSpecObjectMap.containsKey(spec)) {
                        amount = (Double) tunableSpecObjectMap.get(spec);
                    } else {
                        recCpuLimits = model.getCPULimitRecommendation(filteredResultsMap, notifications);
                        amount = recCpuLimits != null ? recCpuLimits.getAmount() : null;
                    }
                    tunableSpecObjectMap.put(spec, amount);
                    break;
                case RecommendationConstants.RecommendationEngine.RuntimeConstants.GPU:
                    recommendationAcceleratorRequestMap = model.getAcceleratorRequestRecommendation(filteredResultsMap, notifications);
                    tunableSpecObjectMap.put(spec, recommendationAcceleratorRequestMap);
                    break;
                case RecommendationConstants.RecommendationEngine.TunablesConstants.MAX_RAM_PERC:
                case RecommendationConstants.RecommendationEngine.TunablesConstants.GC_POLICY:
                case RecommendationConstants.RecommendationEngine.TunablesConstants.QUARKUS_THREAD_POOL_CORE_THREADS:
                    Object recommendationRuntimes = model.getRuntimeRecommendations(metricName, layerName, filteredResultsMap, tunableSpecObjectMap, notifications);
                    tunableSpecObjectMap.put(spec, recommendationRuntimes);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + metricName);
            }
        }

        StringBuilder jvmOptsBuilder = new StringBuilder();
        StringBuilder quarkusBuilder = new StringBuilder();
        Map<String, StringBuilder> envBuilders = new HashMap<>();
        envBuilders.put(KruizeConstants.JSONKeys.JDK_JAVA_OPTIONS, jvmOptsBuilder);
        envBuilders.put(KruizeConstants.JSONKeys.JAVA_OPTIONS, jvmOptsBuilder);
        envBuilders.put(RecommendationConstants.RecommendationEngine.TunablesConstants.QUARKUS_THREAD_POOL_CORE_THREADS, quarkusBuilder);

        for (Map.Entry<TunableSpec, Object> entry : tunableSpecObjectMap.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            TunableSpec entrySpec = entry.getKey();
            String layerName = entrySpec.layerName();
            String metricName = entrySpec.tunableName();
            if (isRuntimeTunable(metricName)) {
                LayerRecommendationHandler handler = LayerRecommendationHandlerRegistry.getInstance().getHandler(layerName);
                if (handler != null) {
                    handler.formatForEnv(metricName, value, envBuilders);
                }
            }
        }

        addIfNotEmpty(runtimeRecommList, KruizeConstants.JSONKeys.JDK_JAVA_OPTIONS, jvmOptsBuilder);
        addIfNotEmpty(runtimeRecommList, KruizeConstants.JSONKeys.JAVA_OPTIONS, jvmOptsBuilder);
        addIfNotEmpty(runtimeRecommList, KruizeConstants.JSONKeys.QUARKUS_THREAD_POOL_CORE_THREADS, quarkusBuilder);

        return runtimeRecommList;
    }

    private static void addIfNotEmpty(List<RecommendationConfigEnv> list, String name, StringBuilder valueBuilder) {
        if (valueBuilder != null && !valueBuilder.toString().isEmpty()) {
            list.add(new RecommendationConfigEnv(name, valueBuilder.toString()));
        }
    }

    private static boolean isRuntimeTunable(String metricName) {
        return RecommendationConstants.RecommendationEngine.TunablesConstants.MAX_RAM_PERC.equals(metricName)
                || RecommendationConstants.RecommendationEngine.TunablesConstants.GC_POLICY.equals(metricName)
                || RecommendationConstants.RecommendationEngine.TunablesConstants.QUARKUS_THREAD_POOL_CORE_THREADS.equals(metricName);
    }
}
