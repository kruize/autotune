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

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * MappedRecommendationForTimestampV1 represents the timestamp-level recommendation structure for v1 API endpoint.
 * This includes current with replicas and nested resources structure.
 */
public class MappedRecommendationForTimestampV1 {

    public MappedRecommendationForTimestampV1() {
        this.current = new HashMap<>();
    }

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> higherLevelNotificationMap;

    @SerializedName(KruizeConstants.JSONKeys.MONITORING_END_TIME)
    private Timestamp monitoringEndTime;

    @SerializedName(KruizeConstants.JSONKeys.CURRENT)
    private HashMap<String, Object> current;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_TERMS)
    private HashMap<String, TermRecommendationsV1> recommendationForTermHashMap;

    public HashMap<Integer, RecommendationNotification> getHigherLevelNotificationMap() {
        return higherLevelNotificationMap;
    }

    public void setHigherLevelNotificationMap(HashMap<Integer, RecommendationNotification> higherLevelNotificationMap) {
        this.higherLevelNotificationMap = higherLevelNotificationMap;
    }

    public Timestamp getMonitoringEndTime() {
        return monitoringEndTime;
    }

    public void setMonitoringEndTime(Timestamp monitoringEndTime) {
        this.monitoringEndTime = monitoringEndTime;
    }

    public HashMap<String, Object> getCurrent() {
        return current;
    }

    public void setCurrent(HashMap<String, Object> current) {
        this.current = current;
    }

    /**
     * Set current replicas
     */
    public void setCurrentReplicas(Integer replicas) {
        if (null == this.current) {
            this.current = new HashMap<>();
        }
        this.current.put(KruizeConstants.JSONKeys.REPLICAS, replicas);
    }

    /**
     * Set current resources (nested structure with limits/requests)
     */
    public void setCurrentResources(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> resources) {
        if (null == this.current) {
            this.current = new HashMap<>();
        }
        this.current.put(KruizeConstants.JSONKeys.RESOURCES, resources);
    }

    public TermRecommendationsV1 getShortTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.SHORT_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.SHORT_TERM);
        return null;
    }

    public void setShortTermRecommendations(TermRecommendationsV1 shortTermRecommendations) {
        if (null != shortTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.SHORT_TERM, shortTermRecommendations);
    }

    public TermRecommendationsV1 getMediumTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.MEDIUM_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.MEDIUM_TERM);
        return null;
    }

    public void setMediumTermRecommendations(TermRecommendationsV1 mediumTermRecommendations) {
        if (null != mediumTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.MEDIUM_TERM, mediumTermRecommendations);
    }

    public TermRecommendationsV1 getLongTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.LONG_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.LONG_TERM);
        return null;
    }

    public void setLongTermRecommendations(TermRecommendationsV1 longTermRecommendations) {
        if (null != longTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.LONG_TERM, longTermRecommendations);
    }

    public HashMap<String, TermRecommendationsV1> getRecommendationForTermHashMap() {
        return recommendationForTermHashMap;
    }

    public void setRecommendationForTermHashMap(HashMap<String, TermRecommendationsV1> recommendationForTermHashMap) {
        this.recommendationForTermHashMap = recommendationForTermHashMap;
    }

    public void setRecommendationForTermHashMap(String term, TermRecommendationsV1 mappedRecommendationForTerm) {
        if (null != term && null != mappedRecommendationForTerm) {
            if (null == this.recommendationForTermHashMap)
                this.recommendationForTermHashMap = new HashMap<>();
            this.recommendationForTermHashMap.put(term, mappedRecommendationForTerm);
        }
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.higherLevelNotificationMap)
            this.higherLevelNotificationMap = new HashMap<>();
        if (null != recommendationNotification)
            if (!this.higherLevelNotificationMap.containsKey(recommendationNotification.getCode()))
                this.higherLevelNotificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }
}
