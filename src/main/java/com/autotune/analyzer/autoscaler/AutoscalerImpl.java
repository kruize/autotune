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

package com.autotune.analyzer.autoscaler;

import com.autotune.analyzer.autoscaler.vpa.VpaAutoscalerImpl;
import com.autotune.analyzer.exceptions.ApplyRecommendationsError;
import com.autotune.analyzer.exceptions.FetchMetricsError;
import com.autotune.analyzer.exceptions.InvalidRecommendationUpdaterType;
import com.autotune.analyzer.kruizeObject.KruizeObject;
import com.autotune.analyzer.recommendations.engine.RecommendationEngine;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.analyzer.utils.AnalyzerErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoscalerImpl implements Autoscaler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscalerImpl.class);

    /**
     * Retrieves an instance of a specific updater implementation based on the provided updater type
     *
     * @param updaterType String the type of updater to retrieve
     * @return RecommendationUpdaterImpl An instance of provided updater type class
     * @throws InvalidRecommendationUpdaterType If the provided updater type doesn't match any valid type of updater.
     */
    @Override
    public AutoscalerImpl getAutoscalerInstance(String updaterType) throws InvalidRecommendationUpdaterType {
        if (AnalyzerConstants.AutoscalerConstants.SupportedUpdaters.VPA.equalsIgnoreCase(updaterType)) {
            return VpaAutoscalerImpl.getInstance();
        } else {
            throw new InvalidRecommendationUpdaterType(String.format(AnalyzerErrorConstants.AutoscalerErrors.UNSUPPORTED_UPDATER_TYPE, updaterType));
        }
    }

    /**
     * Checks whether the necessary updater dependencies are installed or available in the system.
     * @return boolean true if the required updaters are installed, false otherwise.
     */
    @Override
    public boolean isUpdaterInstalled() {
        /*
        * This function will be implemented by specific updater type child classes
        */
        return false;
    }

    /**
     * Generates resource recommendations for a specific experiment based on the experiment's name.
     *
     * @param experimentName String The name of the experiment for which the resource recommendations should be generated.
     * @return KruizeObject containing recommendations
     */
    @Override
    public KruizeObject generateResourceRecommendationsForExperiment(String experimentName) {
        try {
            LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.GENERATING_RECOMMENDATIONS, experimentName);
            // generating latest recommendations for experiment
            RecommendationEngine recommendationEngine = new RecommendationEngine(experimentName, null, null);
            int calCount = 0;
            String validationMessage = recommendationEngine.validate_local();
            if (validationMessage.isEmpty()) {
                KruizeObject kruizeObject = recommendationEngine.prepareRecommendations(calCount, null, null);
                if (kruizeObject.getValidation_data().isSuccess()) {
                    LOGGER.debug(AnalyzerConstants.AutoscalerConstants.InfoMsgs.GENERATED_RECOMMENDATIONS, experimentName);
                    return kruizeObject;
                } else {
                    throw new Exception(kruizeObject.getValidation_data().getMessage());
                }
            } else {
                throw new Exception(validationMessage);
            }
        } catch (Exception | FetchMetricsError e) {
            LOGGER.error(AnalyzerErrorConstants.AutoscalerErrors.GENERATE_RECOMMENDATION_FAILED, experimentName);
            LOGGER.debug(e.getMessage());
            return null;
        }
    }

    /**
     * Applies the resource recommendations contained within the provided KruizeObject
     * This method will take the KruizeObject, which contains the resource recommendations,
     * and apply them to the desired resources.
     *
     * @param kruizeObject KruizeObject containing the resource recommendations to be applied.
     * @throws ApplyRecommendationsError in case of any error.
     */
    @Override
    public void applyResourceRecommendationsForExperiment(KruizeObject kruizeObject) throws ApplyRecommendationsError {
        /*
         * This function will be implemented by specific updater type child classes
         */
    }
}
