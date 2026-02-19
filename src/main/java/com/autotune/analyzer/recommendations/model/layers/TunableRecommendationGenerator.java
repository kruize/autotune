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
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.common.data.metrics.MetricMetadataResults;
import com.autotune.common.data.result.IntervalResults;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Interface for layer-specific tunable recommendation generators.
 * Each layer (Hotspot, Semeru, Quarkus, etc.) implements this interface
 * to provide its own recommendation logic for its tunables.
 */
public interface TunableRecommendationGenerator {
    
    String getLayerName();
    
    /**
     * Generates a recommendation for a specific tunable in this layer.
     * @param tunable The tunable to generate recommendation for
     * @param dependencyValues Map of ALL dependency values using TunableSpec keys
     * @param filteredResultsMap Metrics data for extracting JVM metadata and other runtime info
     * @return RecommendationConfigEnv with the tunable recommendation, or null if cannot generate
     */
    RecommendationConfigEnv generateRecommendation(Tunable tunable, Map<TunableSpec, Object> dependencyValues, Map<Timestamp, IntervalResults> filteredResultsMap);
}
