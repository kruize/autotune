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
import com.autotune.common.data.result.IntervalResults;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Handles recommendation generation for a specific Kruize layer (e.g., hotspot, semeru, quarkus).
 * Each layer implementation encapsulates its own tunable recommendation logic.
 */
public interface LayerRecommendationHandler {

    /**
     * Returns the layer name this handler supports (e.g., "hotspot", "semeru", "quarkus").
     */
    String getLayerName();

    /**
     * Produces a recommendation value for the given tunable name.
     *
     * @param tunableName        name of the tunable (e.g., GCPolicy, MaxRAMPercentage, quarkus.thread-pool.core-threads)
     * @param tunableSpecObjectMap              shared input: read other layers' values via getTunableValue, mem/cpu limits, JVM metadata
     * @param filteredResultsMap
     * @return recommended value, or null if no recommendation can be produced
     */
    Object generateRecommendations(String tunableName, Map<TunableSpec, Object> tunableSpecObjectMap, Map<Timestamp, IntervalResults> filteredResultsMap);

    /**
     * Formats the tunable value for the given env var builder.
     *
     * @param tunableName    name of the tunable
     * @param value         the recommended value (from generateRecommendations)
     * @param envBuilders   map of env var name -> StringBuilder to append to (e.g., JDK_JAVA_OPTIONS, JAVA_OPTIONS, quarkus.thread-pool.core-threads)
     */
    void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders);
}
