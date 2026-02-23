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
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Recommendation handler for the Quarkus framework layer.
 * Produces core-threads recommendation based on CPU limit.
 */
public class QuarkusLayerRecommendationHandler implements LayerRecommendationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLayerRecommendationHandler.class);

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
    public Object generateRecommendations(
            String tunableName,
            Map<TunableSpec, Object> tunableSpecObjectMap,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        switch (tunableName) {
            case RecommendationConstants.RecommendationEngine.TunablesConstants.CORE_THREADS:
                return generateCoreThreadsRecommendation(tunableName, tunableSpecObjectMap, filteredResultsMap);
            default:
                LOGGER.warn("Unknown tunable for Quarkus layer: {}", tunableName);
                return null;
        }
    }

    /**
     * Generates core threads recommendation for Quarkus worker thread pool.
     * <p>
     * <b>Calculation:</b>
     * <ul>
     *   <li>threads = ceil(cpu_cores × THREADS_PER_CORE), where cpu_cores is the container CPU limit</li>
     *   <li>Result is clamped to [MIN_CORE_THREADS, MAX_CORE_THREADS] (default: 1–100)</li>
     * </ul>
     *
     * @param tunableName         the tunable name (CORE_THREADS)
     * @param tunableSpecObjectMap map containing CPU_LIMIT from the container layer
     * @param filteredResultsMap  interval results (unused for this tunable)
     * @return recommended number of core threads, or null if CPU limit is invalid
     */
    private Object generateCoreThreadsRecommendation(
            String tunableName,
            Map<TunableSpec, Object> tunableSpecObjectMap,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        double cpuCores = (Double) RecommendationUtils.getTunableValue(
                tunableSpecObjectMap,AnalyzerConstants.CONTAINER,
                RecommendationConstants.RecommendationEngine.TunablesConstants.CPU_LIMIT);
        if (cpuCores <= 0) {
            LOGGER.warn("Invalid CPU limit for Quarkus: {} cores", cpuCores);
            return null;
        }

        int recommendedThreads = (int) Math.ceil(cpuCores * RecommendationConstants.RecommendationEngine.RuntimeConstants.THREADS_PER_CORE);

        if (recommendedThreads < RecommendationConstants.RecommendationEngine.RuntimeConstants.MIN_CORE_THREADS) {
            LOGGER.debug("Calculated threads ({}) below minimum, using {}", recommendedThreads, RecommendationConstants.RecommendationEngine.RuntimeConstants.MIN_CORE_THREADS);
            recommendedThreads = RecommendationConstants.RecommendationEngine.RuntimeConstants.MIN_CORE_THREADS;
        } else if (recommendedThreads > RecommendationConstants.RecommendationEngine.RuntimeConstants.MAX_CORE_THREADS) {
            LOGGER.warn("Calculated threads ({}) exceeds maximum, capping at {}", recommendedThreads, RecommendationConstants.RecommendationEngine.RuntimeConstants.MAX_CORE_THREADS);
            recommendedThreads = RecommendationConstants.RecommendationEngine.RuntimeConstants.MAX_CORE_THREADS;
        }

        LOGGER.debug("Calculated Quarkus core threads: {} for {} CPU cores ({}x multiplier)",
                recommendedThreads, cpuCores, RecommendationConstants.RecommendationEngine.RuntimeConstants.THREADS_PER_CORE);


        LOGGER.info("Generated Quarkus core threads: {} for CPU: {} cores",
                recommendedThreads, cpuCores);

        return recommendedThreads;
    }

    @Override
    public void formatForEnv(String tunableName, Object value, Map<String, StringBuilder> envBuilders) {
        if (value == null) return;

        if (RecommendationConstants.RecommendationEngine.TunablesConstants.CORE_THREADS.equals(tunableName)) {
            StringBuilder quarkusBuilder = envBuilders.get(RecommendationConstants.RecommendationEngine.TunablesConstants.CORE_THREADS);
            if (quarkusBuilder != null) {
                quarkusBuilder.append(value);
            }
        }
    }
}
