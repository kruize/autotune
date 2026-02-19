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
import com.autotune.analyzer.kruizeLayer.impl.runtime.HotspotLayer;
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.metrics.MetricResults;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT;

/**
 * Generates recommendations for Hotspot JVM tunables.
 */
public class HotspotRecommendationGenerator implements TunableRecommendationGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HotspotRecommendationGenerator.class);
    
    // Track if -server flag has been added (for JDK 8 and below)
    private boolean serverFlagAdded = false;

    private static final double DEFAULT_MAX_RAM_PERCENTAGE = 90.0;
    private static final double MIN_HEAP_PERCENTAGE = 25.0;
    private static final double MEMORY_THRESHOLD_SERIAL_GC = 1792.0;
    private static final double MEMORY_THRESHOLD_G1GC = 4096.0;
    private static final double MEMORY_THRESHOLD_MODERN_GC = 8192.0;
    private static final double DEFAULT_THREAD_MEMORY_MB = 1.0;
    private static final int CPU_CORES_THRESHOLD_SERIAL = 1;
    private static final int CPU_CORES_THRESHOLD_PARALLEL = 2;
    private static final int CPU_CORES_THRESHOLD_MODERN_GC = 4;
    private static final double RAM_PERCENTAGE_THRESHOLD_BELOW_ONE_CPU_CORE = 10.0;
    private static final double RAM_PERCENTAGE_THRESHOLD_ONE_CPU_CORE = 5.0;

    
    // Safety buffer constants for dynamic MaxRAMPercentage calculation
    private static final double SAFETY_BUFFER_BASE_MB = 30.0; // Base safety buffer for OS
    private static final double SAFETY_BUFFER_PERCENTAGE_512MB_WORKLOADS = 0.05;
    private static final double SAFETY_BUFFER_PERCENTAGE_1024MB_WORKLOADS = 0.03;
    private static final double SAFETY_BUFFER_PERCENTAGE_2048MB_WORKLOADS = 0.02;
    private static final double SAFETY_BUFFER_PERCENTAGE_4096MB_WORKLOADS = 0.01;



    // JDK version thresholds for modern GCs
    private static final int JDK_VERSION_SHENANDOAH = 11;
    private static final int JDK_VERSION_ZGC = 17;
    private static final int JDK_VERSION_SERVER_FLAG_THRESHOLD = 8;
    
    @Override
    public String getLayerName() {
        return HotspotLayer.getInstance().getName();
    }
    
    /**
     * Generates the -server flag recommendation for Hotspot JDK 8 and below.
     * @param filteredResultsMap Metrics data for extracting JVM metadata
     * @return Recommendation with -server flag, or null if not needed
     */
    public RecommendationConfigEnv generateServerFlagRecommendation(Map<Timestamp, IntervalResults> filteredResultsMap) {
        
        // Extract JVM metadata from results
        MetricMetadataResults jvmMetadata = RecommendationUtils.getJvmMetadata(filteredResultsMap);
        if (jvmMetadata == null) {
            LOGGER.debug("No JVM metadata available for ServerFlag recommendation");
            return null;
        }
        int jdkVersion = RecommendationUtils.parseJdkMajorVersion(jvmMetadata.getVersion());
        
        // Add -server flag for JDK 8 and below
        if (jdkVersion > 0 && jdkVersion <= JDK_VERSION_SERVER_FLAG_THRESHOLD) {
            LOGGER.info("Adding -server flag for Hotspot JDK {}", jdkVersion);
            return new RecommendationConfigEnv("ServerFlag", "-server");
        }
        
        LOGGER.debug("Skipping -server flag for Hotspot JDK {} (default in JDK 9+)", jdkVersion);
        return null;
    }
    
    @Override
    public RecommendationConfigEnv generateRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {
        
        String tunableName = tunable.getName();

        RecommendationConfigEnv recommendation;
        switch (tunableName) {
            case MAX_RAM_PERC:
                recommendation = generateMaxRAMPercentageRecommendation(tunable, dependencyValues, filteredResultsMap);
                break;
            case GC_POLICY:
                recommendation = generateGCPolicyRecommendation(tunable, dependencyValues, filteredResultsMap);
                break;
            default:
                LOGGER.warn("Unknown tunable for Hotspot layer: {}", tunableName);
                return null;
        }

        if (recommendation != null && !serverFlagAdded) {
            RecommendationConfigEnv serverFlag = generateServerFlagRecommendation(filteredResultsMap);
            if (serverFlag != null) {
                // Prepend -server flag to the recommendation value
                String combinedValue = serverFlag.getValue() + " " + recommendation.getValue();
                recommendation = new RecommendationConfigEnv(recommendation.getName(), combinedValue);
                serverFlagAdded = true;
                LOGGER.info("Prepended -server flag to first Hotspot recommendation");
            } else {
                serverFlagAdded = true; // Mark as checked even if not added
            }
        }
        return recommendation;
    }
    
    /**
     * Generates MaxRAMPercentage recommendation using hybrid approach.
     * @param tunable The MaxRAMPercentage tunable
     * @param dependencyValues Map containing TunableSpec keys with dependency values
     * @param filteredResultsMap Metrics data for dynamic calculation
     * @return Recommendation or null if dependencies missing
     */
    private RecommendationConfigEnv generateMaxRAMPercentageRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        TunableSpec memorySpec = new TunableSpec("container", MEMORY_LIMIT);
        if (dependencyValues == null || !dependencyValues.containsKey(memorySpec)) {
            LOGGER.error("Memory limit not in dependencies for Hotspot MaxRAMPercentage. " +
                    "Container-level memory recommendation must be generated first.");
            return null;
        }
        double containerMemoryBytes = (Double) dependencyValues.get(memorySpec);
        double containerMemoryMB = containerMemoryBytes / (1024 * 1024);
        if (containerMemoryMB <= 0) {
            LOGGER.warn("Invalid memory limit for Hotspot: {}MB", containerMemoryMB);
            return null;
        }

        TunableSpec cpuSpec = new TunableSpec("container", CPU_LIMIT);
        double containerCpuCores = dependencyValues.containsKey(cpuSpec) ? (Double) dependencyValues.get(cpuSpec) : 0.0;

        double maxRamPercentage = calculateMaxRAMPercentage(containerMemoryMB, containerCpuCores, filteredResultsMap);
        LOGGER.info("Generated Hotspot MaxRAMPercentage: {}% for container memory: {}MB", maxRamPercentage, containerMemoryMB);
        
        return new RecommendationConfigEnv(tunable.getName(),String.format("-XX:MaxRAMPercentage=%.1f", maxRamPercentage));
    }
    
    private double calculateMaxRAMPercentage(
            double containerMemoryMB,
            double containerCpuCores,
            Map<Timestamp, IntervalResults> filteredResultsMap) {
        
        if (filteredResultsMap != null && !filteredResultsMap.isEmpty()) {
            Double dynamicPercentage = calculateDynamicMaxRAMPercentage(containerMemoryMB, containerCpuCores, filteredResultsMap);
            if (dynamicPercentage != null) {
                return dynamicPercentage;
            }
        }

       return calculateStaticMaxRAMPercentage(containerMemoryMB, containerCpuCores);
    }
    
    /**
     * Calculates MaxRAMPercentage dynamically from actual JVM metrics collected in filterMap.
    * @param containerMemoryMB Container memory limit in MB
     * @param filteredResultsMap Metrics data collected across all intervals
     * @param containerCpuCores Container CPU limit in cores
     * @return Calculated percentage, or null if metrics insufficient
     */
    private Double calculateDynamicMaxRAMPercentage(
            double containerMemoryMB,double containerCpuCores,
            Map<Timestamp, IntervalResults> filteredResultsMap) {
        
        try {
            if (filteredResultsMap == null || filteredResultsMap.isEmpty()) {
                LOGGER.debug("No interval results available for dynamic calculation");
                return null;
            }
            
            LOGGER.info("Attempting dynamic MaxRAMPercentage calculation using JVM metrics from {} intervals",
                    filteredResultsMap.size());

            double maxJvmTotalNonHeapMemory = 0.0;
            double maxThreads = 0.0;
            double maxDirectBuffers = 0.0;

            for (IntervalResults intervalResults : filteredResultsMap.values()) {
                if (intervalResults.getMetricResultsMap() == null) {
                    continue;
                }
                
                Optional<MetricResults> jvmTotalNonHeapMemoryResults = Optional.ofNullable(
                        intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.jvmTotalNonHeapMemory));
                Optional<MetricResults> threadsResults = Optional.ofNullable(
                        intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.jvmThreadsLive));
                Optional<MetricResults> directBuffersResults = Optional.ofNullable(
                        intervalResults.getMetricResultsMap().get(AnalyzerConstants.MetricName.jvmBufferMemoryDirect));
                
                double jvmTotalNonHeapMemory = jvmTotalNonHeapMemoryResults
                        .map(m -> {
                            double max = m.getAggregationInfoResult().getMax();
                            return max > 0 ? max : m.getAggregationInfoResult().getAvg();
                        })
                        .orElse(0.0);

                double threads = threadsResults
                        .map(m -> {
                            double max = m.getAggregationInfoResult().getMax();
                            return max > 0 ? max : m.getAggregationInfoResult().getAvg();
                        })
                        .orElse(0.0);
                
                double directBuffers = directBuffersResults
                        .map(m -> {
                            double max = m.getAggregationInfoResult().getMax();
                            return max > 0 ? max : m.getAggregationInfoResult().getAvg();
                        })
                        .orElse(0.0);

                maxJvmTotalNonHeapMemory = Math.max(maxJvmTotalNonHeapMemory, jvmTotalNonHeapMemory);
                maxThreads = Math.max(maxThreads, threads);
                maxDirectBuffers = Math.max(maxDirectBuffers, directBuffers);
            }
            
            boolean hasAnyMetric = (maxJvmTotalNonHeapMemory > 0 || maxThreads > 0 || maxDirectBuffers > 0);
            if (!hasAnyMetric) {
                LOGGER.warn("No JVM metrics collected across {} intervals. Dynamic calculation not possible. " +
                        "Ensure application exposes JVM metrics (jvm_memory_committed_bytes, jvm_threads_live_threads, jvm_buffer_memory_used_bytes)",
                        filteredResultsMap.size());
                return null;
            }
            
            // Convert bytes to MB and calculate total non-heap footprint
            double JvmTotalNonHeapMemoryMB = maxJvmTotalNonHeapMemory / (1024 * 1024);
            double threadsMB = maxThreads * DEFAULT_THREAD_MEMORY_MB;
            double directBuffersMB = maxDirectBuffers / (1024 * 1024);
            double totalNativeMB = JvmTotalNonHeapMemoryMB + threadsMB + directBuffersMB;
            
            // If we couldn't get meaningful metrics, return null to trigger fallback
            if (totalNativeMB < 10.0) {
                LOGGER.debug("Non-heap footprint too small ({}MB), metrics may be incomplete", totalNativeMB);
                return null;
            }
            
            LOGGER.debug("Non-heap footprint (max across intervals): JvmTotalNonHeapMemoryMB={}MB, Threads={}MB, DirectBuffers={}MB, Total={}MB",
                    JvmTotalNonHeapMemoryMB, threadsMB, directBuffersMB, totalNativeMB);

            // Apply safety buffer
            double safetyBufferPercentage;
            if (containerMemoryMB <= 512 ){
                safetyBufferPercentage = SAFETY_BUFFER_PERCENTAGE_512MB_WORKLOADS;
            } else if (containerMemoryMB <= 1024) {
                safetyBufferPercentage = SAFETY_BUFFER_PERCENTAGE_1024MB_WORKLOADS;
            } else if (containerMemoryMB <= 2048) {
                safetyBufferPercentage = SAFETY_BUFFER_PERCENTAGE_2048MB_WORKLOADS;
            } else {
                safetyBufferPercentage = SAFETY_BUFFER_PERCENTAGE_4096MB_WORKLOADS;
            }
            double safetyBuffer = SAFETY_BUFFER_BASE_MB + (containerMemoryMB * safetyBufferPercentage);
            double availableForHeap = containerMemoryMB - (totalNativeMB + safetyBuffer);

            if (availableForHeap < containerMemoryMB * (MIN_HEAP_PERCENTAGE / 100.0)) {
                LOGGER.info("Dynamic MaxRAMPercentage: Container={}MB, NonHeap={}MB, SafetyBuffer={}MB, AvailableHeap={}MB",
                        containerMemoryMB, totalNativeMB, safetyBuffer, availableForHeap);
                    return MIN_HEAP_PERCENTAGE;
            }

            double maxRamPercentage = (availableForHeap / containerMemoryMB) * 100;
            if (maxRamPercentage < MIN_HEAP_PERCENTAGE) {
                LOGGER.warn("Calculated maxram percentage ({}MB) is below minimum {}% for container ({}MB)",
                        maxRamPercentage, MIN_HEAP_PERCENTAGE, containerMemoryMB);
                maxRamPercentage = MIN_HEAP_PERCENTAGE;
            }

            double recommendedThresholdPercentage = calculateStaticMaxRAMPercentage(containerMemoryMB, containerCpuCores);
            double recommendedMaxRAMPercentage = Math.min(maxRamPercentage, recommendedThresholdPercentage);
            LOGGER.info("Calculated MaxRAMPercentage ({}%) , ThresholdPercentage ({}%) for {}MB container",
                        maxRamPercentage, recommendedThresholdPercentage, containerMemoryMB);


            if (recommendedMaxRAMPercentage > DEFAULT_MAX_RAM_PERCENTAGE) {
                LOGGER.debug("Calculated MaxRAMPercentage ({}%) exceeds maximum recommended ({}%), capping at maximum", recommendedMaxRAMPercentage, DEFAULT_MAX_RAM_PERCENTAGE);
                recommendedMaxRAMPercentage = DEFAULT_MAX_RAM_PERCENTAGE;
            }

            LOGGER.info("Dynamic MaxRAMPercentage: Container={}MB, NonHeap={}MB, SafetyBuffer={}MB, " +
                    "AvailableHeap={}MB, Recommended={}%, Final={}%",
                    containerMemoryMB, totalNativeMB, safetyBuffer, availableForHeap, recommendedThresholdPercentage, recommendedMaxRAMPercentage);
            
            return recommendedMaxRAMPercentage;
            
        } catch (Exception e) {
            LOGGER.warn("Error calculating dynamic MaxRAMPercentage: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculates MaxRAMPercentage using static formula based on memory and CPU limits.
     * @param containerMemoryMB Container memory limit in MB
     * @param containerCpuCores Container CPU limit in cores
     * @return Calculated percentage
     */
    private double calculateStaticMaxRAMPercentage(double containerMemoryMB, double containerCpuCores) {
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
            maxRamPercentage -= RAM_PERCENTAGE_THRESHOLD_BELOW_ONE_CPU_CORE;
        } else if (containerCpuCores < 2.0) {
            maxRamPercentage -= RAM_PERCENTAGE_THRESHOLD_ONE_CPU_CORE;
        }
        return Math.max(MIN_HEAP_PERCENTAGE, maxRamPercentage);
    }
    
    
    /**
     * Generates GC Policy recommendation
     * Selects appropriate GC based on heap size, CPU cores, and JDK version.
     * @param tunable The GC Policy tunable
     * @param dependencyValues Map containing all required dependencies with TunableSpec keys
     * @param filteredResultsMap Metrics data for extracting JVM metadata
     * @return Recommendation or null if dependencies missing
     */
    private RecommendationConfigEnv generateGCPolicyRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        if (dependencyValues == null) {
            LOGGER.error("No dependencies provided for Hotspot GC Policy");
            return null;
        }

        TunableSpec cpuSpec = new TunableSpec("container", CPU_LIMIT);
        if (!dependencyValues.containsKey(cpuSpec)) {
            LOGGER.error("CPU limit not in dependencies for Hotspot GC Policy. " +
                    "Container-level CPU recommendation must be generated first.");
            return null;
        }
        double containerCpuCores = (Double) dependencyValues.get(cpuSpec);
        
        TunableSpec memorySpec = new TunableSpec("container", MEMORY_LIMIT);
        if (!dependencyValues.containsKey(memorySpec)) {
            LOGGER.error("Memory limit not in dependencies for Hotspot GC Policy. " +
                    "Container-level memory recommendation must be generated first.");
            return null;
        }
        double containerMemoryBytes = (Double) dependencyValues.get(memorySpec);
        double containerMemoryMB = containerMemoryBytes / (1024 * 1024);
        
        if (containerCpuCores <= 0 || containerMemoryMB <= 0) {
            LOGGER.warn("Invalid container resources for Hotspot: cpu={} cores, memory={}MB",
                    containerCpuCores, containerMemoryMB);
            return null;
        }
        
        TunableSpec maxRamSpec = new TunableSpec("hotspot", MAX_RAM_PERC);
        double maxRamPercentage;
        if (dependencyValues.containsKey(maxRamSpec)) {
            maxRamPercentage = (Double) dependencyValues.get(maxRamSpec);
            LOGGER.debug("Using generated MaxRAMPercentage from dependencies: {}%", maxRamPercentage);
        } else {
            maxRamPercentage = calculateMaxRAMPercentage(containerMemoryMB, containerCpuCores, filteredResultsMap);
            LOGGER.warn("MaxRAMPercentage not in dependencies, calculated fallback: {}%", maxRamPercentage);
        }
        
        double effectiveHeapMB = (containerMemoryMB * maxRamPercentage) / 100.0;
        
        MetricMetadataResults jvmMetadata = RecommendationUtils.getJvmMetadata(filteredResultsMap);
        if (jvmMetadata != null) {
            LOGGER.debug("Using JVM metadata for Hotspot GC selection: version={}, runtime={}, vendor={}",
                    jvmMetadata.getVersion(), jvmMetadata.getRuntime(), jvmMetadata.getVendor());
        } else {
            LOGGER.debug("No JVM metadata available, using default GC selection logic");
        }
        
        String gcPolicy = selectGCPolicy(effectiveHeapMB, containerCpuCores, jvmMetadata);
        
        LOGGER.info("Generated Hotspot GC Policy: {} for heap: {}MB, cpu: {} cores",
                gcPolicy, effectiveHeapMB, containerCpuCores);
        
        return new RecommendationConfigEnv(tunable.getName(), gcPolicy);
    }
    
    /**
     * Selects Hotspot GC policy
     */
    private String selectGCPolicy(double heapSizeMB, double cpuCores, MetricMetadataResults jvmMetadata) {
        int cores = (int) Math.ceil(cpuCores);
        int jdkMajorVersion = (jvmMetadata != null) ? RecommendationUtils.parseJdkMajorVersion(jvmMetadata.getVersion()) : 0;
        
        // For single core, use SerialGC
        if (cores <= CPU_CORES_THRESHOLD_SERIAL) {
            LOGGER.debug("Selected Hotspot SerialGC: cores={}, heapMB={}, jdk={}", cores, heapSizeMB, jdkMajorVersion);
            return "-XX:+UseSerialGC";
        }
        // For 2 cores and small heap, use ParallelGC
        if (cores <= CPU_CORES_THRESHOLD_PARALLEL && heapSizeMB <= MEMORY_THRESHOLD_G1GC) {
            LOGGER.debug("Selected Hotspot ParallelGC: cores={}, heapMB={}, jdk={}", cores, heapSizeMB, jdkMajorVersion);
            return "-XX:+ParallelGC";
        }
        
        // For very large heaps with sufficient cores, consider modern low-latency GCs
        if (heapSizeMB >= MEMORY_THRESHOLD_G1GC && cores >= CPU_CORES_THRESHOLD_PARALLEL) {
            // JDK 17+: ZGC is production-ready and offers ultra-low latency
            if (jdkMajorVersion >= JDK_VERSION_ZGC) {
                LOGGER.info("Selected Hotspot ZGC: cores={}, heapMB={}, jdk={} (ultra-low latency)",
                        cores, heapSizeMB, jdkMajorVersion);
                return "-XX:+UseZGC";
            }
            
            // JDK 11-16: Shenandoah offers low latency
            if (jdkMajorVersion >= JDK_VERSION_SHENANDOAH) {
                LOGGER.info("Selected Hotspot ShenandoahGC: cores={}, heapMB={}, jdk={} (low latency)",
                        cores, heapSizeMB, jdkMajorVersion);
                return "-XX:+UseShenandoahGC";
            }
        }
        
        // Default to G1GC for large heaps (4-8GB) or when JDK version is unknown/old
        LOGGER.debug("Selected Hotspot G1GC: cores={}, heapMB={}, jdk={} (balanced performance)",
                cores, heapSizeMB, jdkMajorVersion);
        return "-XX:+UseG1GC";
    }
}
