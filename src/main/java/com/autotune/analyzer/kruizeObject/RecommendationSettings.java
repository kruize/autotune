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
    @SerializedName("recommendation_types_config")
    private RecommendationTypesConfig recommendationTypesConfig;

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

    public RecommendationTypesConfig getRecommendationTypesConfig() {
        return recommendationTypesConfig;
    }

    public void setRecommendationTypesConfig(RecommendationTypesConfig recommendationTypesConfig) {
        this.recommendationTypesConfig = recommendationTypesConfig;
    }

    /**
     * Returns true if the given recommendation type should be generated.
     * Supports both legacy flat array format and new nested object format.
     * When both are null/empty, all types are enabled (default behavior).
     *
     * @param type One of KruizeConstants.RecommendationTypes (RESOURCE, RUNTIME, ACCELERATOR)
     * @return true if the type should be generated
     */
    public boolean isRecommendationTypeEnabled(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        
        // If new format is provided, use it
        if (recommendationTypesConfig != null) {
            if (KruizeConstants.RecommendationTypes.RESOURCE.equalsIgnoreCase(type)) {
                return recommendationTypesConfig.hasResourcesEnabled();
            } else if (KruizeConstants.RecommendationTypes.RUNTIME.equalsIgnoreCase(type)) {
                return recommendationTypesConfig.hasRuntimesEnabled();
            } else if (KruizeConstants.RecommendationTypes.ACCELERATOR.equalsIgnoreCase(type)) {
                return recommendationTypesConfig.hasAcceleratorsEnabled();
            }
            return false;
        }
        
        // Fall back to legacy format
        if (recommendationTypes == null || recommendationTypes.isEmpty()) {
            return true; // Default: all types enabled
        }
        return recommendationTypes.stream()
                .anyMatch(t -> t != null && t.equalsIgnoreCase(type));
    }

    /**
     * Returns true if a specific resource type should be generated.
     *
     * @param resourceType One of KruizeConstants.ResourceTypes (CPU, MEMORY)
     * @return true if the resource type should be generated
     */
    public boolean isResourceTypeEnabled(String resourceType) {
        if (resourceType == null || resourceType.isEmpty()) {
            return false;
        }
        
        // If new format is provided, use it
        if (recommendationTypesConfig != null) {
            return recommendationTypesConfig.isResourceEnabled(resourceType);
        }
        
        // Fall back to legacy format - if "resource" is enabled, all resource types are enabled
        return isRecommendationTypeEnabled(KruizeConstants.RecommendationTypes.RESOURCE);
    }

    /**
     * Returns true if runtime recommendations for the specified layer should be generated.
     * Supports both new nested format and legacy flat array format.
     *
     * @param layerName The specific runtime layer name (e.g., "hotspot", "quarkus", "semeru")
     * @return true if recommendations for this layer should be generated
     */
    public boolean isRuntimeLayerEnabled(String layerName) {
        if (layerName == null || layerName.isEmpty()) {
            return false;
        }
        
        // If new format is provided, use it
        if (recommendationTypesConfig != null) {
            return recommendationTypesConfig.isRuntimeEnabled(layerName);
        }
        
        // Fall back to legacy format
        if (recommendationTypes == null || recommendationTypes.isEmpty()) {
            return true; // Default: all types enabled
        }
        
        // Check if the specific layer is enabled
        boolean specificLayerEnabled = recommendationTypes.stream()
                .anyMatch(t -> t != null && t.equalsIgnoreCase(layerName));
        
        // Check if generic "runtime" is enabled (enables all runtime layers)
        boolean genericRuntimeEnabled = recommendationTypes.stream()
                .anyMatch(t -> t != null && t.equalsIgnoreCase(KruizeConstants.RecommendationTypes.RUNTIME));
        
        return specificLayerEnabled || genericRuntimeEnabled;
    }

    /**
     * Returns true if a specific accelerator type should be generated.
     *
     * @param acceleratorType One of KruizeConstants.AcceleratorTypes (GPU)
     * @return true if the accelerator type should be generated
     */
    public boolean isAcceleratorTypeEnabled(String acceleratorType) {
        if (acceleratorType == null || acceleratorType.isEmpty()) {
            return false;
        }
        
        // If new format is provided, use it
        if (recommendationTypesConfig != null) {
            return recommendationTypesConfig.isAcceleratorEnabled(acceleratorType);
        }
        
        // Fall back to legacy format - if "accelerator" is enabled, all accelerator types are enabled
        return isRecommendationTypeEnabled(KruizeConstants.RecommendationTypes.ACCELERATOR);
    }

    @Override
    public String toString() {
        return "RecommendationSettings{" +
                "threshold=" + threshold +
                ", modelSettings=" + modelSettings +
                ", termSettings=" + termSettings +
                ", recommendationTypes=" + recommendationTypes +
                ", recommendationTypesConfig=" + recommendationTypesConfig +
                '}';
    }
}
