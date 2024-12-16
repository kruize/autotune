/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

package com.autotune.analyzer.recommendations.updater;

import com.autotune.analyzer.exceptions.ApplyRecommendationsError;
import com.autotune.analyzer.exceptions.InvalidRecommendationUpdaterType;
import com.autotune.analyzer.kruizeObject.KruizeObject;

/**
 * This interface defines the abstraction for updating resource recommendations in a system.
 * Implementing classes will provide the logic to update resources with recommendations for a specific resources,
 * such as CPU, memory, or any other resources that require periodic or dynamic adjustments.
 *
 * The RecommendationUpdater interface is designed to be extended by different updater classes.
 * For example, vpaUpdaterImpl for updating resources with recommendations related to CPU and memory resources.
 */

public interface RecommendationUpdater {
    /**
     * Retrieves an instance of a specific updater implementation based on the provided updater type
     *
     * @param updaterType String the type of updater to retrieve
     * @return RecommendationUpdaterImpl An instance of provided updater type class
     * @throws InvalidRecommendationUpdaterType If the provided updater type doesn't match any valid type of updater.
     */
    RecommendationUpdaterImpl getUpdaterInstance(String updaterType) throws InvalidRecommendationUpdaterType;

    /**
     * Checks whether the necessary updater dependencies are installed or available in the system.
     *
     * @return boolean true if the required updaters are installed, false otherwise.
     */
    boolean isUpdaterInstalled();

    /**
     * Generates resource recommendations for a specific experiment based on the experiment's name.
     *
     * @param experimentName String The name of the experiment for which the resource recommendations should be generated.
     * @return KruizeObject containing recommendations
     */
    KruizeObject generateResourceRecommendationsForExperiment(String experimentName);

    /**
     * Applies the resource recommendations contained within the provided KruizeObject
     * This method will take the KruizeObject, which contains the resource recommendations,
     * and apply them to the desired resources.
     *
     * @param kruizeObject KruizeObject containing the resource recommendations to be applied.
     * @throws ApplyRecommendationsError in case of any error.
     */
    void applyResourceRecommendationsForExperiment(KruizeObject kruizeObject) throws ApplyRecommendationsError;
}
