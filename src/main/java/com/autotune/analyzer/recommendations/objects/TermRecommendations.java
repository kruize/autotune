package com.autotune.analyzer.recommendations.objects;

import com.autotune.analyzer.recommendations.RecommendationConstants;
import com.autotune.analyzer.recommendations.RecommendationNotification;
import com.autotune.utils.KruizeConstants;
import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;
import java.util.HashMap;

public class TermRecommendations implements MappedRecommendationForTerm {

    public TermRecommendations() {
    }

    @SerializedName(KruizeConstants.JSONKeys.DURATION_IN_HOURS)
    private double durationInHrs;

    public TermRecommendations(RecommendationConstants.RecommendationTerms recommendationTerm) {
        this.durationInHrs = recommendationTerm.getDuration();
    }
    @SerializedName(KruizeConstants.JSONKeys.NOTIFICATIONS)
    private HashMap<Integer, RecommendationNotification> termLevelNotificationMap;

    @SerializedName(KruizeConstants.JSONKeys.MONITORING_START_TIME)
    private Timestamp monitoringStartTime;

    @SerializedName(KruizeConstants.JSONKeys.RECOMMENDATION_ENGINES)
    private HashMap<String, MappedRecommendationForModel> recommendationForEngineHashMap;

    @Override
    public HashMap<Integer, RecommendationNotification> getNotifications() {
        return this.termLevelNotificationMap;
    }

    @Override
    public Timestamp getMonitoringStartTime() {
        return this.monitoringStartTime;
    }

    @Override
    public double getDurationInHrs() {
        return this.durationInHrs;
    }

    public void setDurationInHrs(double durationInHrs) {
        this.durationInHrs = durationInHrs;
    }

    @Override
    public MappedRecommendationForModel getRecommendationByEngine(String EngineName) {
        return null;
    }

    public void setTermLevelNotificationMap(HashMap<Integer, RecommendationNotification> termLevelNotificationMap) {
        this.termLevelNotificationMap = termLevelNotificationMap;
    }

    public void setMonitoringStartTime(Timestamp monitoringStartTime) {
        this.monitoringStartTime = monitoringStartTime;
    }

    public HashMap<String, MappedRecommendationForModel> getRecommendationForEngineHashMap() {
        return recommendationForEngineHashMap;
    }

    public void setRecommendationForEngineHashMap(HashMap<String, MappedRecommendationForModel> recommendationForEngineHashMap) {
        this.recommendationForEngineHashMap = recommendationForEngineHashMap;
    }

    public void setRecommendationForEngineHashMap(String engineName, MappedRecommendationForModel mappedRecommendationForModel) {
        if (null != engineName && null != mappedRecommendationForModel) {
            if (null == this.recommendationForEngineHashMap)
                this.recommendationForEngineHashMap = new HashMap<>();
            this.recommendationForEngineHashMap.put(engineName, mappedRecommendationForModel);
        }
    }

    public MappedRecommendationForModel getCostRecommendations() {
        if (null != this.recommendationForEngineHashMap && this.recommendationForEngineHashMap.containsKey(KruizeConstants.JSONKeys.COST))
            return this.recommendationForEngineHashMap.get(KruizeConstants.JSONKeys.COST);
        return null;
    }

    public MappedRecommendationForModel getPerformanceRecommendations() {
        if (null != this.recommendationForEngineHashMap && this.recommendationForEngineHashMap.containsKey(KruizeConstants.JSONKeys.PERFORMANCE))
            return this.recommendationForEngineHashMap.get(KruizeConstants.JSONKeys.PERFORMANCE);
        return null;
    }

    public void addNotification(RecommendationNotification recommendationNotification) {
        if (null == this.termLevelNotificationMap)
            this.termLevelNotificationMap = new HashMap<>();

        if (null != recommendationNotification)
            this.termLevelNotificationMap.put(recommendationNotification.getCode(), recommendationNotification);
    }
}
