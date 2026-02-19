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
import com.autotune.analyzer.kruizeLayer.impl.framework.QuarkusLayer;
import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.utils.RecommendationUtils;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.result.IntervalResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Map;

import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.CPU_LIMIT;
import static com.autotune.analyzer.utils.AnalyzerConstants.LayerConstants.TunablesConstants.CORE_THREADS;

/**
 * Generates recommendations for Quarkus framework tunables.
 */
public class QuarkusRecommendationGenerator implements TunableRecommendationGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusRecommendationGenerator.class);
    
    private static final int THREADS_PER_CORE = 1; // Default multiplier
    private static final int MIN_CORE_THREADS = 1;
    private static final int MAX_CORE_THREADS = 100;
    
    @Override
    public String getLayerName() {
        return QuarkusLayer.getInstance().getName();
    }
    
    @Override
    public RecommendationConfigEnv generateRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues,
            Map<Timestamp, IntervalResults> filteredResultsMap) {

        String tunableName = tunable.getName();
        
        switch (tunableName) {
            case CORE_THREADS:
                return generateCoreThreadsRecommendation(tunable, dependencyValues);
            default:
                LOGGER.warn("Unknown tunable for Quarkus layer: {}", tunableName);
                return null;
        }
    }
    
    /**
     * Generates core threads recommendation for Quarkus worker thread pool.
     */
    private RecommendationConfigEnv generateCoreThreadsRecommendation(
            Tunable tunable,
            Map<TunableSpec, Object> dependencyValues) {
        
        TunableSpec cpuSpec = new TunableSpec("container", CPU_LIMIT);
        if (dependencyValues == null || !dependencyValues.containsKey(cpuSpec)) {
            LOGGER.error("CPU limit not in dependencies for Quarkus core threads");
            return null;
        }

        double containerCpuCores = (Double) dependencyValues.get(cpuSpec);
        if (containerCpuCores <= 0) {
            LOGGER.warn("Invalid CPU limit for Quarkus: {} cores", containerCpuCores);
            return null;
        }

        int recommendedThreads = (int) Math.ceil(containerCpuCores * THREADS_PER_CORE);

        if (recommendedThreads < MIN_CORE_THREADS) {
            LOGGER.debug("Calculated threads ({}) below minimum, using {}", recommendedThreads, MIN_CORE_THREADS);
            recommendedThreads = MIN_CORE_THREADS;
        } else if (recommendedThreads > MAX_CORE_THREADS) {
            LOGGER.warn("Calculated threads ({}) exceeds maximum, capping at {}", recommendedThreads, MAX_CORE_THREADS);
            recommendedThreads = MAX_CORE_THREADS;
        }

        LOGGER.debug("Calculated Quarkus core threads: {} for {} CPU cores ({}x multiplier)",
                recommendedThreads, containerCpuCores, THREADS_PER_CORE);

        
        LOGGER.info("Generated Quarkus core threads: {} for CPU: {} cores",
                recommendedThreads, containerCpuCores);
        
        return new RecommendationConfigEnv("QUARKUS_THREAD_POOL_CORE_THREADS", String.valueOf(recommendedThreads));
    }
}
