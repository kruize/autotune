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
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;

import static com.autotune.analyzer.recommendations.utils.RecommendationUtils.getJvmMetricMetadataFromFilteredResults;
import static com.autotune.analyzer.recommendations.utils.RecommendationUtils.getTunableValue;

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
            case AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC:
                recommendation = generateHotspotMaxRAMPercentageRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
                break;
            case AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY:
                recommendation = generateHotspotGCPolicyRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
                break;
            default:
                LOGGER.warn("Unknown tunable for Hotspot layer: {}", tunableName);
                return null;
        }
        return recommendation;
    }

    /**
     * Generates MaxRAMPercentage recommendation
     * @param tunableName The tunable
     * @param tunableSpecObjectMap Map containing TunableSpec keys with dependency values
     * @param filteredResultsMap Metrics data
     * @return Recommendation or null if dependencies missing
     */
    private Double generateHotspotMaxRAMPercentageRecommendation(
            String tunableName,
            Map<TunableSpec, Object> tunableSpecObjectMap,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        double containerMemoryBytes = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT);
        double containerMemoryMB = containerMemoryBytes / (1024 * 1024);
        if (containerMemoryMB <= 0) {
            LOGGER.warn("Invalid memory limit for Hotspot: {}MB", containerMemoryMB);
            return null;
        }

        double containerCpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT);

        double maxRamPercentage;
        if (containerMemoryMB <= 256.0) {
            maxRamPercentage = 50.0; // Tiny: JVM needs half for internal tasks
        } else if (containerMemoryMB <= 512.0) {
            maxRamPercentage = 60.0; // Small
        } else if (containerMemoryMB <= 4096.0) {
            maxRamPercentage = 75.0; // Medium
        } else if (containerMemoryMB <= 8192.0) {
            maxRamPercentage = 80.0;
        } else {
            maxRamPercentage = 85.0;
        }

        if (containerCpuCores < 1.0) {
            maxRamPercentage -= AnalyzerConstants.RecommendationConstants.RAM_PERCENTAGE_THRESHOLD_BELOW_ONE_CPU_CORE;
        } else if (containerCpuCores < 2.0) {
            maxRamPercentage -= AnalyzerConstants.RecommendationConstants.RAM_PERCENTAGE_THRESHOLD_ONE_CPU_CORE;
        }
        LOGGER.info("Generated MaxRAMPercentage: {}% for container memory: {}MB", maxRamPercentage, containerMemoryMB);

        return maxRamPercentage;

    }

    private String generateHotspotGCPolicyRecommendation(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        MetricMetadataResults jvmMetadataMetrics = RecommendationUtils.getJvmMetricMetadataFromFilteredResults(filteredResultsMap);
        int jdkMajorVersion = (jvmMetadataMetrics != null) ? RecommendationUtils.parseMajorVersion(jvmMetadataMetrics.getVersion()) : 0;


        double memLimit = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT);
        double memLimitMB = memLimit / (1024 * 1024);
        double cpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT);
        int cores = (int) Math.round(cpuCores);
        double maxRAMPercent = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap, AnalyzerConstants.LayerConstants.HOTSPOT_LAYER,
                AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC);
        double jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);
        String gcPolicy;

        // For single core, use SerialGC
        if (cores <= AnalyzerConstants.RecommendationConstants.CPU_CORES_THRESHOLD_SERIAL) {
            LOGGER.debug("Selected Hotspot SerialGC: cores={}, heapMB={}, jdk={}", cores, jvmHeapSizeMB, jdkMajorVersion);
            gcPolicy = AnalyzerConstants.RecommendationConstants.GC_SERIAL;
            return gcPolicy;
        }
        // For 2 cores and small heap, use ParallelGC
        if (cores <= AnalyzerConstants.RecommendationConstants.CPU_CORES_THRESHOLD_PARALLEL &&
                jvmHeapSizeMB <= AnalyzerConstants.RecommendationConstants.MEMORY_THRESHOLD_G1GC) {
            LOGGER.debug("Selected Hotspot ParallelGC: cores={}, heapMB={}, jdk={}", cores, jvmHeapSizeMB, jdkMajorVersion);
            gcPolicy = AnalyzerConstants.RecommendationConstants.GC_PARALLEL;
            return gcPolicy;
        }

        // For very large heaps with sufficient cores, consider modern low-latency GCs
        if (jvmHeapSizeMB >= AnalyzerConstants.RecommendationConstants.MEMORY_THRESHOLD_G1GC && cores >= AnalyzerConstants.RecommendationConstants.CPU_CORES_THRESHOLD_PARALLEL) {
            // JDK 17+: ZGC is production-ready and offers ultra-low latency
            if (jdkMajorVersion >= AnalyzerConstants.RecommendationConstants.JDK_VERSION_ZGC) {
                LOGGER.info("Selected Hotspot ZGC: cores={}, heapMB={}, jdk={} (ultra-low latency)",
                        cores, jvmHeapSizeMB, jdkMajorVersion);
                gcPolicy = AnalyzerConstants.RecommendationConstants.GC_ZGC;
                return gcPolicy;
            }

            // JDK 11-16: Shenandoah offers low latency
            if (jdkMajorVersion >= AnalyzerConstants.RecommendationConstants.JDK_VERSION_SHENANDOAH) {
                LOGGER.info("Selected Hotspot ShenandoahGC: cores={}, heapMB={}, jdk={} (low latency)",
                        cores, jvmHeapSizeMB, jdkMajorVersion);
                gcPolicy = AnalyzerConstants.RecommendationConstants.GC_SHENANDOAH;
                return gcPolicy;
            }
        }

        // Default to G1GC for large heaps (>4GB) or when JDK version is unknown/old
        LOGGER.debug("Selected Hotspot G1GC: cores={}, heapMB={}, jdk={} (balanced performance)",
                cores, jvmHeapSizeMB, jdkMajorVersion);
        gcPolicy =  AnalyzerConstants.RecommendationConstants.GC_G1GC;
        return gcPolicy;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        RecommendationUtils.formatForJVMEnv(tunableName, value, envBuilders);
    }
}
