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

package com.autotune.analyzer.recommendations.v1;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * MappedRecommendationForModelV1 represents the recommendation structure for v1 API endpoint.
 * This includes replicas in config and variation with nested resources structure.
 */
public class MappedRecommendationForModelV1 {

    public MappedRecommendationForModelV1() {
        this.confidence_level = 0.0;
        this.config = new ConfigV1();
        this.variation = new HashMap<>();
        this.notificationHashMap = new HashMap<>();
    }

    @SerializedName(KruizeConstants.JSONKeys.CONFIDENCE_LEVEL)
    private double confidence_level;
    
    @SerializedName(KruizeConstants.JSONKeys.CONFIG)
    private ConfigV1 config;
    
    @SerializedName(KruizeConstants.JSONKeys.VARIATION)
    private HashMap<String, Object> variation;

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> notificationHashMap;

    public double getConfidence_level() {
        return confidence_level;
    }

    public void setConfidence_level(double confidence_level) {
        this.confidence_level = confidence_level;
    }

    public ConfigV1 getConfig() {
        return config;
    }

    public void setConfig(ConfigV1 config) {
        this.config = config;
    }

    public HashMap<String, Object> getVariation() {
        return variation;
    }

    public void setVariation(HashMap<String, Object> variation) {
        this.variation = variation;
    }

    /**
     * Set replicas variation
     */
    public void setReplicasVariation(Integer replicas) {
        if (null == this.variation) {
            this.variation = new HashMap<>();
        }
        this.variation.put(KruizeConstants.JSONKeys.REPLICAS, replicas);
    }

    /**
     * Set resources variation (nested structure with limits/requests)
     */
    public void setResourcesVariation(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources) {
        if (null == this.variation) {
            this.variation = new HashMap<>();
        }
        this.variation.put(KruizeConstants.JSONKeys.RESOURCES, resources);
    }

    public HashMap<Integer, RecommendationNotification> getNotificationHashMap() {
        return notificationHashMap;
    }

    public void setNotificationHashMap(HashMap<Integer, RecommendationNotification> notificationHashMap) {
        this.notificationHashMap = notificationHashMap;
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.notificationHashMap)
            this.notificationHashMap = new HashMap<>();
        if (null != recommendationNotification)
            if (!this.notificationHashMap.containsKey(recommendationNotification.getCode()))
                this.notificationHashMap.put(recommendationNotification.getCode(), recommendationNotification);
    }
}
