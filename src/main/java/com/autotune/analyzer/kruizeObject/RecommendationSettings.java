/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.kruizeObject;

import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecommendationSettings {
    private Double threshold;
    @SerializedName(KruizeConstants.JSONKeys.MODEL_SETTINGS)
    private ModelSettings modelSettings;
    @SerializedName(KruizeConstants.JSONKeys.TERM_SETTINGS)
    private TermSettings termSettings;
    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_TYPES)
    private List<String> recommendationTypes;

    public RecommendationSettings(){}

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public ModelSettings getModelSettings() {
        return modelSettings;
    }

    public void setModelSettings(ModelSettings modelSettings) {
        this.modelSettings = modelSettings;
    }

    public TermSettings getTermSettings() {
        return termSettings;
    }

    public void setTermSettings(TermSettings termSettings) {
        this.termSettings = termSettings;
    }

    public List<String> getRecommendationTypes() {
        return recommendationTypes;
    }

    public void setRecommendationTypes(List<String> recommendationTypes) {
        this.recommendationTypes = recommendationTypes;
    }

    /**
     * Returns true if the given recommendation type should be generated.
     * When recommendationTypes is null or empty, all types are enabled (default behavior).
     *
     * @param type One of KruizeConstants.RecommendationTypes (RESOURCE, RUNTIME, ACCELERATOR)
     * @return true if the type should be generated
     */
    public boolean isRecommendationTypeEnabled(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        if (recommendationTypes == null || recommendationTypes.isEmpty()) {
            return true; // Default: all types enabled
        }
        return recommendationTypes.stream()
                .anyMatch(t -> t != null && t.equalsIgnoreCase(type));
    }

    @Override
    public String toString() {
        return "RecommendationSettings{" +
                "threshold=" + threshold +
                ", modelSettings=" + modelSettings +
                ", termSettings=" + termSettings +
                ", recommendationTypes=" + recommendationTypes +
                '}';
    }
}
