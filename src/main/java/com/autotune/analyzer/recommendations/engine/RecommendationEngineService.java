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

package com.autotune.analyzer.recommendations.engine;

import com.autotune.analyzer.recommendations.RecommendationConfigEnv;
import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.recommendations.model.RecommendationModel;
import com.autotune.analyzer.recommendations.objects.MappedRecommendationForModel;
import com.autotune.analyzer.recommendations.term.Terms;
import com.autotune.analyzer.utils.AnalyzerConstants;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service interface providing a contract for recommendation processors.
 * This interface decouples processors from the full RecommendationEngine implementation,
 * exposing only the methods needed for recommendation generation.
 */
public interface RecommendationEngineService {

    /**
     * Gets the list of recommendation models to be used for generating recommendations.
     *
     * @return List of RecommendationModel instances
     */
    List<RecommendationModel> getModels();

    /**
     * Populates recommendation data for a given term and model.
     *
     * @param termEntry                        The term entry containing term name and configuration
     * @param recommendationModel              The model to populate with recommendations
     * @param notifications                    List of notifications to be added
     * @param internalMapToPopulate           Map containing current and recommended values
     * @param numPods                         Number of pods
     * @param cpuThreshold                    CPU threshold value
     * @param memoryThreshold                 Memory threshold value
     * @param recommendationAcceleratorRequestMap Map of accelerator recommendations (can be null)
     * @param runtimeListToPopulate           List of runtime recommendations (can be null)
     * @return true if population was successful, false otherwise
     */
    boolean populateRecommendation(Map.Entry<String, Terms> termEntry,
                                   MappedRecommendationForModel recommendationModel,
                                   ArrayList<RecommendationNotification> notifications,
                                   HashMap<String, RecommendationConfigItem> internalMapToPopulate,
                                   int numPods,
                                   double cpuThreshold,
                                   double memoryThreshold,
                                   Map<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> recommendationAcceleratorRequestMap,
                                   List<RecommendationConfigEnv> runtimeListToPopulate);

    /**
     * Returns the experiment name.
     * Used for associating recommendations with the correct experiment.
     *
     * @return The experiment name
     */
    String getExperimentName();

    /**
     * Returns the end timestamp of the monitoring interval for which recommendations are generated.
     * Used to identify the recommendation period.
     *
     * @return The interval end time
     */
    Timestamp getInterval_end_time();
}
