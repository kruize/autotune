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

package com.autotune.analyzer.recommendations;

import com.autotune.analyzer.kruizeLayer.impl.TunableSpec;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.metrics.MetricMetadataResults;

import java.util.Map;

/**
 * Implementation of LayerRecommendationInput backed by the shared recommendation map.
 * Handlers read from this to access container limits and other layers' outputs.
 */
public class MapBackedLayerRecommendationInput implements LayerRecommendationInput {

    private final Map<TunableSpec, Object> recommendations;
    private final MetricMetadataResults metricMetadata;
    private final String effectiveLayer;

    public MapBackedLayerRecommendationInput(Map<TunableSpec, Object> recommendations,
                                              MetricMetadataResults metricMetadata,
                                              String effectiveLayer) {
        this.recommendations = recommendations;
        this.metricMetadata = metricMetadata;
        this.effectiveLayer = effectiveLayer != null ? effectiveLayer : AnalyzerConstants.AutotuneConfigConstants.LAYER_HOTSPOT;
    }

    @Override
    public Object getTunableValue(String layerName, String tunableName) {
        if (recommendations == null || layerName == null || tunableName == null) {
            return null;
        }
        return recommendations.get(new TunableSpec(layerName, tunableName));
    }

    @Override
    public double getMemLimit() {
        Object v = getTunableValue(AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER,
                AnalyzerConstants.MetricNameConstants.MEMORY_LIMIT);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return 0.0;
    }

    @Override
    public double getCpuLimit() {
        Object v = getTunableValue(AnalyzerConstants.AutotuneConfigConstants.LAYER_CONTAINER,
                AnalyzerConstants.MetricNameConstants.CPU_LIMIT);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return 0.0;
    }

    @Override
    public MetricMetadataResults getMetricMetadata() {
        return metricMetadata;
    }

    @Override
    public String getEffectiveLayer() {
        return effectiveLayer;
    }
}
