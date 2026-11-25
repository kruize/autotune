/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.autotune.analyzer.recommendations.model;

import com.autotune.analyzer.kruizeObject.ResourceScopeSettings;
import com.autotune.analyzer.recommendations.RecommendationConstants.RecommendationScope;
import com.autotune.utils.KruizeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static com.autotune.utils.KruizeConstants.PerformanceBasedRecommendationConstants.PERFORMANCE_RECOMMENDATION_TUNABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Unit tests for the per-model default resource scopes.
 */
class DefaultResourceScopeSettingsTest {

    @Test
    @DisplayName("Cost and performance defaults should be all for cpu and memory")
    void costAndPerformanceDefaultsShouldBeAll() {
        List<RecommendationModel> models = List.of(
                new CostBasedRecommendationModel(KruizeConstants.CostBasedRecommendationConstants.COST_RECOMMENDATION_TUNABLES),
                new PerformanceBasedRecommendationModel(PERFORMANCE_RECOMMENDATION_TUNABLES));

        for (RecommendationModel model : models) {
            ResourceScopeSettings defaults = model.getDefaultResourceScopeSettings();
            assertEquals(List.of("all"), defaults.getCpu(), model.getModelName());
            assertEquals(List.of("all"), defaults.getMemory(), model.getModelName());
            assertEquals(EnumSet.of(RecommendationScope.ALL), defaults.getParsedCpuScopes(), model.getModelName());
            assertEquals(EnumSet.of(RecommendationScope.ALL), defaults.getParsedMemoryScopes(), model.getModelName());
        }
    }

    @Test
    @DisplayName("Each call should return a fresh default object that callers can not corrupt")
    void defaultsShouldBeIndependentInstances() {
        RecommendationModel model = new PerformanceBasedRecommendationModel(PERFORMANCE_RECOMMENDATION_TUNABLES);
        assertNotSame(model.getDefaultResourceScopeSettings(), model.getDefaultResourceScopeSettings());
    }
}
