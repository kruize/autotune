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
package com.autotune.analyzer.recommendations.model.layers;

import com.autotune.analyzer.kruizeLayer.Tunable;
import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.kruizeLayer.impl.runtime.SemeruLayer;
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.result.IntervalResults;
import com.autotune.analyzer.recommendations.model.layers.HotspotRecommendationGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;

import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT;

/**
 * Generates recommendations for Semeru (Eclipse OpenJ9) JVM tunables.
 */
public class SemeruRecommendationGenerator implements TunableRecommendationGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemeruRecommendationGenerator.class);

    private static final double DEFAULT_MAX_RAM_PERCENTAGE = 75.0;
    private static final double MEMORY_256MB = 256.0;
    private static final double MEMORY_512MB = 512.0;
    private static final double MEMORY_THRESHOLD_BALANCED_GC = 4096.0; // > 4 GB for balanced GC
    private static final int CPU_CORES_THRESHOLD = 2;
    
    @Override
    public String getLayerName() {
        return SemeruLayer.getInstance().getName();
    }
    
    @Override
    public RecommendationConfigEnv generateRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {
        
        String tunableName = tunable.getName();
        
        switch (tunableName) {
            case MAX_RAM_PERC:
                return generateMaxRAMPercentageRecommendation(tunable, dependencyValues, filteredResultsMap);
            case GC_POLICY:
                return generateGCPolicyRecommendation(tunable, dependencyValues);
            default:
                LOGGER.warn("Unknown tunable for Semeru layer: {}", tunableName);
                return null;
        }
    }
    
    /**
     * Generates MaxRAMPercentage recommendation for Semeru/OpenJ9.
     * Uses similar strategy to Hotspot but can be tuned differently if needed.
     */
    private RecommendationConfigEnv generateMaxRAMPercentageRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        RecommendationConfigEnv recommendation;
        // Using the HotSpot Code to calculate MaxRAMPercentage
        HotspotRecommendationGenerator tunableRec = new HotspotRecommendationGenerator();
        recommendation = tunableRec.generateRecommendation(tunable,dependencyValues,filteredResultsMap);
        return recommendation;
    }
    
    /**
     * Generates GC Policy recommendation for Semeru/OpenJ9.
     * OpenJ9 has different GC policies than Hotspot.
     */
    private RecommendationConfigEnv generateGCPolicyRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues) {

        if (dependencyValues == null) {
            LOGGER.error("No dependencies provided for Semeru GC Policy");
            return null;
        }

        TunableSpec cpuSpec = new TunableSpec("container", CPU_LIMIT);
        if (!dependencyValues.containsKey(cpuSpec)) {
            LOGGER.error("CPU limit not in dependencies for Semeru GC Policy");
            return null;
        }
        double containerCpuCores = (Double) dependencyValues.get(cpuSpec);
        
        TunableSpec memorySpec = new TunableSpec("container", MEMORY_LIMIT);
        if (!dependencyValues.containsKey(memorySpec)) {
            LOGGER.error("Memory limit not in dependencies for Semeru GC Policy");
            return null;
        }
        double containerMemoryBytes = (Double) dependencyValues.get(memorySpec);
        double containerMemoryMB = containerMemoryBytes / (1024 * 1024);

        if (containerCpuCores <= 0 || containerMemoryMB <= 0) {
            LOGGER.warn("Invalid container resources for Semeru: cpu={} cores, memory={}MB", containerCpuCores, containerMemoryMB);
            return null;
        }
        
        TunableSpec maxRamSpec = new TunableSpec("semeru", MAX_RAM_PERC);
        double maxRamPercentage = (Double) dependencyValues.get(maxRamSpec);
        // Calculate effective heap size
        double effectiveHeapMB = (containerMemoryMB * maxRamPercentage) / 100.0;
        
        MetricMetadataResults jvmMetadata = null;
        if (dependencyValues.containsKey(AnalyzerConstants.AutotuneObjectConstants.JVM_METADATA)) {
            jvmMetadata = (MetricMetadataResults) dependencyValues.get(AnalyzerConstants.AutotuneObjectConstants.JVM_METADATA);
            LOGGER.debug("Using JVM metadata for Semeru GC selection: version={}, runtime={}, vendor={}",
                    jvmMetadata.getVersion(), jvmMetadata.getRuntime(), jvmMetadata.getVendor());
        }
        
        // Select GC policy for OpenJ9
        String gcPolicy = selectGCPolicy(effectiveHeapMB, containerCpuCores, jvmMetadata);
        LOGGER.info("Generated Semeru GC Policy: {} for heap: {}MB, cpu: {} cores", gcPolicy, effectiveHeapMB, containerCpuCores);
        
        return new RecommendationConfigEnv(tunable.getName(), gcPolicy);
    }
    
    /**
     * Selects Semeru/OpenJ9 GC policy based on heap size, CPU cores, and JVM version.
     * TODO: Implementation.
     * This is just a placeholder for now.
     */
    private String selectGCPolicy(double heapSizeMB, double cpuCores, MetricMetadataResults jvmMetadata) {
        int cores = (int) Math.ceil(cpuCores);
        int jdkMajorVersion = (jvmMetadata != null) ? RecommendationUtils.parseJdkMajorVersion(jvmMetadata.getVersion()) : 0;
        
        // For single core or small heaps, use gencon (default, efficient)
        if (cores < CPU_CORES_THRESHOLD || heapSizeMB < MEMORY_THRESHOLD_BALANCED_GC) {
            LOGGER.debug("Selected Semeru gencon GC: cores={}, heapMB={}, jdk={}", cores, heapSizeMB, jdkMajorVersion);
            return "-Xgcpolicy:gencon";
        }
        // For larger heaps (>4GB) with multiple cores, use balanced GC
        LOGGER.debug("Selected Semeru balanced GC: cores={}, heapMB={}, jdk={}", cores, heapSizeMB, jdkMajorVersion);
        return "-Xgcpolicy:balanced";
    }
}
