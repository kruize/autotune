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
        Double memLimit = (Double) getTunableValue(tunableSpecObjectMap, AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER, AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT);
        Double cpuLimit = (Double) getTunableValue(tunableSpecObjectMap, AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER, AnalyzerConstants.MetricNameConstants.CPU_LIMIT);

        if (AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC.equals(tunableName)) {
            return AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
        }

        if (AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY.equals(tunableName)) {
            Double jvmHeapSizeMB = null;
            double maxRamPercentage = AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
            //TODO: update the below call based on the new logic
            return decideGCPolicy(jvmHeapSizeMB, maxRamPercentage, memLimit, cpuLimit);
        }

        return null;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        RecommendationUtils.formatForJVMEnv(tunableName, value, envBuilders);
    }

    private String decideGCPolicy(Double jvmHeapSizeMB, double maxRAMPercent, double memLimit, double cpuCores) {
        if (jvmHeapSizeMB == null || jvmHeapSizeMB == 0) {
            double memLimitMB = memLimit / (1024 * 1024);
            jvmHeapSizeMB = Math.ceil((maxRAMPercent / 100) * memLimitMB);
        }

        if (jvmHeapSizeMB >= 4096 && cpuCores > 1) {
            return AnalyzerConstants.LayerConstants.GC_BALANCED;
        }
        return AnalyzerConstants.LayerConstants.GC_GENCON;
    }
}
