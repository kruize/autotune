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

import static com.autotune.analyzer.recommendations.utils.RecommendationUtils.getTunableValue;

/**
 * Recommendation handler for the Semeru JVM layer.
 * Produces GC policy (-Xgcpolicy) and MaxRAMPercentage recommendations.
 */
public class SemeruLayerRecommendationHandler implements LayerRecommendationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemeruLayerRecommendationHandler.class);

    private static final SemeruLayerRecommendationHandler INSTANCE = new SemeruLayerRecommendationHandler();

    public static SemeruLayerRecommendationHandler getInstance() {
        return INSTANCE;
    }

    private SemeruLayerRecommendationHandler() {
    }

    @Override
    public String getLayerName() {
        return AnalyzerConstants.AutotuneConfigConstants.LAYER_SEMERU;
    }

    @Override
    public Object generateRecommendations(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        Object recommendation;
        switch (tunableName) {
            case AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC:
                recommendation = generateSemeruMaxRAMPercentageRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
                break;
            case AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY:
                recommendation = generateSemeruGCPolicyRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
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
    private Object generateSemeruMaxRAMPercentageRecommendation(
            String tunableName,
            Map<TunableSpec, Object> tunableSpecObjectMap,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        // Using HotSpot Layer recommendation function only
        return new HotspotLayerRecommendationHandler().generateRecommendations(tunableName,tunableSpecObjectMap, filteredResultsMap);

    }

    private Object generateSemeruGCPolicyRecommendation(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        double memLimit = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap,AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.MEMORY_LIMIT);
        double memLimitMB = memLimit / (1024 * 1024);
        double cpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap,AnalyzerConstants.CONTAINER,
                AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT);
        int cores = (int) Math.round(cpuCores);
        double maxRAMPercent = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap,AnalyzerConstants.LayerConstants.SEMERU_LAYER,
                AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC);
        double jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);
        String gcPolicy;

        // For single core or small heaps, use gencon (default, efficient)
        if (cores < AnalyzerConstants.RecommendationConstants.CPU_CORES_THRESHOLD_PARALLEL ||
                jvmHeapSizeMB < AnalyzerConstants.RecommendationConstants.MEMORY_THRESHOLD_BALANCED_GC) {
            LOGGER.debug("Selected Semeru gencon GC: cores={}, heapMB={}", cores, jvmHeapSizeMB);
            gcPolicy = AnalyzerConstants.RecommendationConstants.GC_GENCON;
            return gcPolicy;
        }
        // For larger heaps (>4GB) with multiple cores, use balanced GC
        LOGGER.debug("Selected Semeru balanced GC: cores={}, heapMB={} ", cores, jvmHeapSizeMB);
        gcPolicy = AnalyzerConstants.RecommendationConstants.GC_BALANCED;
        return gcPolicy;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        RecommendationUtils.formatForJVMEnv(tunableName, value, envBuilders);
    }
}
