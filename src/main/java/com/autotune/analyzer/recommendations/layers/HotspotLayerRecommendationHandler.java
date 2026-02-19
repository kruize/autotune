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

    private HotspotLayerRecommendationHandler() {
    }

    @Override
    public String getLayerName() {
        return AnalyzerConstants.AutotuneConfigConstants.LAYER_HOTSPOT;
    }

    @Override
    public Object generateRecommendations(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap) {

        Double memLimits = (Double) getTunableValue(tunableSpecObjectMap, AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER, AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT);
        Double cpuLimits = (Double) getTunableValue(tunableSpecObjectMap, AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER, AnalyzerConstants.MetricNameConstants.CPU_LIMIT);

        if (AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC.equals(tunableName)) {
            return AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
        }

        if (AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY.equals(tunableName)) {
            MetricMetadataResults metricMetadata = getJvmMetricMetadataFromFilteredResults(filteredResultsMap);
            String jdkVersion = metricMetadata.getVersion();
            if (jdkVersion == null || jdkVersion.isEmpty()) {
                LOGGER.warn("JVM version is null or empty (layerName=hotspot)");
                return null;
            }
            Double jvmHeapSizeMB = null;
            double maxRamPercentage = AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
            //TODO: update the below call based on the new logic
            return decideGCPolicy(jvmHeapSizeMB, maxRamPercentage, memLimits, cpuLimits, jdkVersion);
        }

        return null;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        RecommendationUtils.formatForJVMEnv(tunableName, value, envBuilders);
    }

    private String decideGCPolicy(Double jvmHeapSizeMB, double maxRAMPercent, double memLimit, double cpuCores, String jdkVersionStr) {
        int jdkVersion = RecommendationUtils.parseMajorVersion(jdkVersionStr);

        if (jvmHeapSizeMB == null || jvmHeapSizeMB == 0) {
            double memLimitMB = memLimit / (1024 * 1024);
            jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);
        }

        if (cpuCores <= 1 && jvmHeapSizeMB < 4096) {
            return AnalyzerConstants.LayerConstants.GC_SERIAL;
        } else if (cpuCores > 1 && jvmHeapSizeMB < 4096) {
            return AnalyzerConstants.LayerConstants.GC_PARALLEL;
        } else if (jvmHeapSizeMB >= 4096) {
            if (jdkVersion >= 17) {
                return AnalyzerConstants.LayerConstants.GC_ZGC;
            } else if (jdkVersion >= 11) {
                return AnalyzerConstants.LayerConstants.GC_SHENANDOAH;
            } else {
                return AnalyzerConstants.LayerConstants.GC_G1GC;
            }
        } else {
            return AnalyzerConstants.LayerConstants.GC_G1GC;
        }
    }
}
