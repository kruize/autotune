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

package com.autotune.analyzer.recommendations.layers;

import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.recommendations.LayerRecommendationHandler;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Recommendation handler for the Hotspot (OpenJDK) JVM layer.
 * Produces GC policy and MaxRAMPercentage recommendations.
 */
public class HotspotLayerRecommendationHandler implements LayerRecommendationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotspotLayerRecommendationHandler.class);

    private static final HotspotLayerRecommendationHandler INSTANCE = new HotspotLayerRecommendationHandler();

    public static HotspotLayerRecommendationHandler getInstance() {
        return INSTANCE;
    }

    public HotspotLayerRecommendationHandler() {
    }

    @Override
    public String getLayerName() {
        return AnalyzerConstants.AutotuneConfigConstants.LAYER_HOTSPOT;
    }

    @Override
    public Object generateRecommendations(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        Object recommendation;
        switch (tunableName) {
            case RecommendationConstants.RecommendationEngine.TunablesConstants.MAX_RAM_PERC:
                recommendation = generateHotspotMaxRAMPercentageRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
                break;
            case RecommendationConstants.RecommendationEngine.TunablesConstants.GC_POLICY:
                recommendation = generateHotspotGCPolicyRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
                break;
            default:
                LOGGER.warn("Unknown tunable for Hotspot layer: {}", tunableName);
                return null;
        }
        return recommendation;
    }

    /**
     * Generates MaxRAMPercentage recommendation for Hotspot JVM (-XX:MaxRAMPercentage).
     * <p>
     * <b>Calculation:</b>
     * <ul>
     *   <li>Base: 50% if container memory ≤ 512MB, else 80%</li>
     *   <li>If CPU cores &lt; 1: subtract 10%</li>
     *   <li>If CPU cores &lt; 2 (and ≥ 1): subtract 5%</li>
     * </ul>
     * Example: 1GB memory, 2 cores → 80%; 256MB memory, 0.5 cores → 40%.
     *
     * @param tunableName         the tunable name (MAX_RAM_PERC)
     * @param tunableSpecObjectMap map containing MEMORY_LIMIT and CPU_LIMIT from the container layer
     * @param filteredResultsMap  interval results (unused for this tunable)
     * @return recommended MaxRAMPercentage (0–100), or null if memory limit is invalid
     */
    public Double generateHotspotMaxRAMPercentageRecommendation(
            String tunableName,
            Map<TunableSpec, Object> tunableSpecObjectMap,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        double containerMemoryBytes = (Double) RecommendationUtils.getTunableValue(
                    tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.MEMORY_LIMIT);

        double containerMemoryMB = containerMemoryBytes / (1024 * 1024);
        if (containerMemoryMB <= 0) {
            LOGGER.warn("Invalid memory limit for Hotspot: {}MB", containerMemoryMB);
            return null;
        }

        double containerCpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.CPU_LIMIT);

        double maxRamPercentage;
        if (containerMemoryMB <= RecommendationConstants.RecommendationEngine.RuntimeConstants.RAM_PERCENTAGE_THRESHOLD_512MB) {
            maxRamPercentage = 50.0;
        } else {
            maxRamPercentage = 80.0;
        }

        if (containerCpuCores < RecommendationConstants.RecommendationEngine.RuntimeConstants.CPU_CORES_THRESHOLD_SERIAL) {
            maxRamPercentage -= RecommendationConstants.RecommendationEngine.RuntimeConstants.RAM_PERCENTAGE_THRESHOLD_BELOW_ONE_CPU_CORE;
        } else if (containerCpuCores < RecommendationConstants.RecommendationEngine.RuntimeConstants.CPU_CORES_THRESHOLD_PARALLEL) {
            maxRamPercentage -= RecommendationConstants.RecommendationEngine.RuntimeConstants.RAM_PERCENTAGE_THRESHOLD_ONE_CPU_CORE;
        }
        LOGGER.debug("Generated MaxRAMPercentage: {}% for container memory: {}MB", maxRamPercentage, containerMemoryMB);

        return maxRamPercentage;

    }

    private String generateHotspotGCPolicyRecommendation(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        MetricMetadataResults jvmMetadataMetrics = RecommendationUtils.getJvmMetricMetadataFromFilteredResults(filteredResultsMap);
        int jdkMajorVersion = (jvmMetadataMetrics != null) ? RecommendationUtils.parseMajorVersion(jvmMetadataMetrics.getVersion()) : 0;

        double memLimit = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.MEMORY_LIMIT);
        double memLimitMB = memLimit / (1024 * 1024);
        double cpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.CPU_LIMIT);
        int cores = (int) Math.ceil(cpuCores);
        double maxRAMPercent = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.LayerConstants.HOTSPOT_LAYER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.MAX_RAM_PERC);
        double jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);

        String gcPolicy;

        // For single core, use SerialGC
        if (cores <= RecommendationConstants.RecommendationEngine.RuntimeConstants.CPU_CORES_THRESHOLD_SERIAL) {
            LOGGER.debug("Selected Hotspot SerialGC: cores={}, heapMB={}, jdk={}", cores, jvmHeapSizeMB, jdkMajorVersion);
            gcPolicy = RecommendationConstants.RecommendationEngine.RuntimeConstants.GC_SERIAL;
            return gcPolicy;
        }
        // For 2 cores and small heap, use ParallelGC
        if (cores <= RecommendationConstants.RecommendationEngine.RuntimeConstants.CPU_CORES_THRESHOLD_PARALLEL &&
                jvmHeapSizeMB <= RecommendationConstants.RecommendationEngine.RuntimeConstants.MEMORY_THRESHOLD_G1GC) {
            LOGGER.debug("Selected Hotspot ParallelGC: cores={}, heapMB={}, jdk={}", cores, jvmHeapSizeMB, jdkMajorVersion);
            gcPolicy = RecommendationConstants.RecommendationEngine.RuntimeConstants.GC_PARALLEL;
            return gcPolicy;
        }

        // For very large heaps with sufficient cores, consider modern low-latency GCs
        if (jvmHeapSizeMB >= RecommendationConstants.RecommendationEngine.RuntimeConstants.MEMORY_THRESHOLD_G1GC) {
            // JDK 17+: ZGC is production-ready and offers ultra-low latency
            if (jdkMajorVersion >= RecommendationConstants.RecommendationEngine.RuntimeConstants.JDK_VERSION_ZGC) {
                LOGGER.debug("Selected Hotspot ZGC: cores={}, heapMB={}, jdk={} (ultra-low latency)",
                        cores, jvmHeapSizeMB, jdkMajorVersion);
                gcPolicy = RecommendationConstants.RecommendationEngine.RuntimeConstants.GC_ZGC;
                return gcPolicy;
            }

            // JDK 11-16: Shenandoah offers low latency
            if (jdkMajorVersion >= RecommendationConstants.RecommendationEngine.RuntimeConstants.JDK_VERSION_SHENANDOAH) {
                LOGGER.debug("Selected Hotspot ShenandoahGC: cores={}, heapMB={}, jdk={} (low latency)",
                        cores, jvmHeapSizeMB, jdkMajorVersion);
                gcPolicy = RecommendationConstants.RecommendationEngine.RuntimeConstants.GC_SHENANDOAH;
                return gcPolicy;
            }
        }

        // Default to G1GC for large heaps (>4GB) or when JDK version is unknown/old
        LOGGER.debug("Selected Hotspot G1GC: cores={}, heapMB={}, jdk={} (balanced performance)",
                cores, jvmHeapSizeMB, jdkMajorVersion);
        gcPolicy =  RecommendationConstants.RecommendationEngine.RuntimeConstants.GC_G1GC;
        return gcPolicy;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        RecommendationUtils.formatForJVMEnv(tunableName, value, envBuilders);
    }
}
