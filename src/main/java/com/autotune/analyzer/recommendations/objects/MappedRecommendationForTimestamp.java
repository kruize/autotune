package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConfigItem;
import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class MappedRecommendationForTimestamp {

    public MappedRecommendationForTimestamp() {

    }

    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationConstants.RecommendationNotification> higherLevelNotificationMap;

    @SerializedName(KruizeConstants.JSONKeys.MONITORING_END_TIME)
    private Timestamp monitoringEndTime;

    @SerializedName(KruizeConstants.JSONKeys.CURRENT)
    private HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_TERMS)
    private HashMap<String, MappedRecommendationForTerm> recommendationForTermHashMap;

    public HashMap<Integer, RecommendationConstants.RecommendationNotification> getHigherLevelNotificationMap() {
        return higherLevelNotificationMap;
    }

    public void setHigherLevelNotificationMap(HashMap<Integer, RecommendationConstants.RecommendationNotification> higherLevelNotificationMap) {
        this.higherLevelNotificationMap = higherLevelNotificationMap;
    }

    public Timestamp getMonitoringEndTime() {
        return monitoringEndTime;
    }

    public void setMonitoringEndTime(Timestamp monitoringEndTime) {
        this.monitoringEndTime = monitoringEndTime;
    }

    public HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> getCurrentConfig() {
        return currentConfig;
    }

    public void setCurrentConfig(HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> currentConfig) {
        this.currentConfig = currentConfig;
    }

    public MappedRecommendationForTerm getShortTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.SHORT_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.SHORT_TERM);
        return null;
    }

    public void setShortTermRecommendations(MappedRecommendationForTerm shortTermRecommendations) {
        if (null != shortTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.SHORT_TERM, shortTermRecommendations);
    }

    public MappedRecommendationForTerm getMediumTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.MEDIUM_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.MEDIUM_TERM);
        return null;
    }

    public void setMediumTermRecommendations(MappedRecommendationForTerm mediumTermRecommendations) {
        if (null != mediumTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.MEDIUM_TERM, mediumTermRecommendations);
    }

    public MappedRecommendationForTerm getLongTermRecommendations() {
        if (null != this.recommendationForTermHashMap && this.recommendationForTermHashMap.containsKey(KruizeConstants.JSONKeys.LONG_TERM))
            return this.recommendationForTermHashMap.get(KruizeConstants.JSONKeys.LONG_TERM);
        return null;
    }

    public void setLongTermRecommendations(MappedRecommendationForTerm longTermRecommendations) {
        if (null != longTermRecommendations && null != recommendationForTermHashMap)
            this.recommendationForTermHashMap.put(KruizeConstants.JSONKeys.LONG_TERM, longTermRecommendations);
    }

    public HashMap<String, MappedRecommendationForTerm> getRecommendationForTermHashMap() {
        return recommendationForTermHashMap;
    }

    public void setRecommendationForTermHashMap(HashMap<String, MappedRecommendationForTerm> recommendationForTermHashMap) {
        this.recommendationForTermHashMap = recommendationForTermHashMap;
    }

    public void setRecommendationForTermHashMap(String term, MappedRecommendationForTerm mappedRecommendationForTerm) {
        if (null != term && null != mappedRecommendationForTerm) {
            if (null == this.recommendationForTermHashMap)
                this.recommendationForTermHashMap = new HashMap<>();
            this.recommendationForTermHashMap.put(term, mappedRecommendationForTerm);
        }
    }
}
