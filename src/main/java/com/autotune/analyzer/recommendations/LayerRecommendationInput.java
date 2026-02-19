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

import com.autotune.common.data.metrics.MetricMetadataResults;

/**
 * Input provided to layer recommendation handlers.
 * Enables inter-handler communication: handlers can read other layers' tunable values
 *
 */
public interface LayerRecommendationInput {

    /**
     * Reads any layer's tunable value. Enables frameworks to use JVM runtime outputs
     * (e.g., Hotspot's MaxRAMPercentage) when computing their own recommendations.
     *
     * @param layerName  layer name (e.g., "container", "hotspot", "semeru")
     * @param tunableName tunable name (e.g., "memoryLimit", "MaxRAMPercentage")
     * @return the value, or null if not present
     */
    Object getTunableValue(String layerName, String tunableName);

    /**
     * container memory limit in bytes.
     */
    double getMemLimit();

    /**
     * container CPU limit in cores.
     */
    double getCpuLimit();

    /**
     * JVM metadata (runtime, version, vendor). Null for non-JVM stacks.
     */
    MetricMetadataResults getMetricMetadata();

    /**
     * Effective JVM layer (hotspot/semeru) from runtime detection.
     */
    String getEffectiveLayer();

    /**
     * JDK version string, or null if not available.
     */
    default String getJdkVersion() {
        MetricMetadataResults m = getMetricMetadata();
        return m != null ? m.getVersion() : null;
    }
}
