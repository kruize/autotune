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
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.util.Map;

/**
 * Recommendation handler for the Quarkus framework layer.
 * Produces core-threads recommendation based on CPU limit.
 */
public class QuarkusLayerRecommendationHandler implements LayerRecommendationHandler {

    private static final QuarkusLayerRecommendationHandler INSTANCE = new QuarkusLayerRecommendationHandler();

    public static QuarkusLayerRecommendationHandler getInstance() {
        return INSTANCE;
    }

    private QuarkusLayerRecommendationHandler() {
    }

    @Override
    public String getLayerName() {
        return AnalyzerConstants.AutotuneConfigConstants.LAYER_QUARKUS;
    }

    @Override
    public Object getRecommendation(String tunableName, LayerRecommendationInput input) {
        if (AnalyzerConstants.LayerConstants.TunablesConstants.CORE_THREADS.equals(tunableName)) {
            return (int) Math.ceil(input.getCpuLimit());
        }
        return null;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        if (value == null) return;

        if (AnalyzerConstants.LayerConstants.TunablesConstants.CORE_THREADS.equals(tunableName)) {
            StringBuilder quarkusBuilder = envBuilders.get(AnalyzerConstants.LayerConstants.TunablesConstants.CORE_THREADS);
            if (quarkusBuilder != null) {
                quarkusBuilder.append(value);
            }
        }
    }
}
