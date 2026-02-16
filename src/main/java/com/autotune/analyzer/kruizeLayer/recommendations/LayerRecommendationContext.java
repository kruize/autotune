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

package com.autotune.analyzer.kruizeLayer.recommendations;

import com.autotune.common.data.metrics.MetricMetadataResults;

/**
 * Context passed to layer recommendation handlers for producing tunable recommendations.
 * Contains container-level values and JVM metadata needed by runtime layers.
 */
public class LayerRecommendationContext {
    private final double memLimit;
    private final double cpuLimit;
    private final MetricMetadataResults metricMetadata;
    private final String effectiveLayer;  // "hotspot" or "semeru" derived from runtime

    public LayerRecommendationContext(double memLimit, double cpuLimit,
                                       MetricMetadataResults metricMetadata,
                                       String effectiveLayer) {
        this.memLimit = memLimit;
        this.cpuLimit = cpuLimit;
        this.metricMetadata = metricMetadata;
        this.effectiveLayer = effectiveLayer;
    }

    public double getMemLimit() {
        return memLimit;
    }

    public double getCpuLimit() {
        return cpuLimit;
    }

    public MetricMetadataResults getMetricMetadata() {
        return metricMetadata;
    }

    public String getEffectiveLayer() {
        return effectiveLayer;
    }

    public String getJdkVersion() {
        return metricMetadata != null ? metricMetadata.getVersion() : null;
    }

    @Override
    public String toString() {
        return "LayerRecommendationContext{" +
                "memLimit=" + memLimit +
                ", cpuLimit=" + cpuLimit +
                ", metricMetadata=" + metricMetadata +
                ", effectiveLayer='" + effectiveLayer + '\'' +
                '}';
    }
}
