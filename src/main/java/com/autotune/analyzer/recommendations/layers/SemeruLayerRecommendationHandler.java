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

import com.autotune.analyzer.recommendations.LayerRecommendationHandler;
import com.autotune.analyzer.recommendations.LayerRecommendationInput;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Recommendation handler for the Semeru/OpenJ9 JVM layer.
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
    public Object getRecommendation(String tunableName, LayerRecommendationInput input) {
        if (!AnalyzerConstants.AutotuneConfigConstants.LAYER_SEMERU.equalsIgnoreCase(input.getEffectiveLayer())) {
            LOGGER.debug("Semeru handler: effectiveLayer '{}' does not match layerName 'semeru'", input.getEffectiveLayer());
            return null;
        }

        if (AnalyzerConstants.LayerConstants.TunablesConstants.MAX_RAM_PERC.equals(tunableName)) {
            return AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
        }

        if (AnalyzerConstants.LayerConstants.TunablesConstants.GC_POLICY.equals(tunableName)) {
            Double jvmHeapSizeMB = null;
            double maxRamPercentage = AnalyzerConstants.HotspotConstants.MAX_RAM_PERCENTAGE_VALUE;
            return decideGCPolicy(jvmHeapSizeMB, maxRamPercentage, input.getMemLimit(), input.getCpuLimit());
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
            return "-Xgcpolicy:balanced";
        }
        return "-Xgcpolicy:gencon";
    }
}
